package com.tailoris.marketing.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tailoris.common.dto.PageRequest;
import com.tailoris.common.dto.PageResponse;
import com.tailoris.common.exception.BusinessException;
import com.tailoris.common.lock.DistributedLock;
import com.tailoris.common.util.SnowflakeIdGenerator;
import com.tailoris.marketing.dto.CouponCreateRequest;
import com.tailoris.marketing.entity.CouponTemplate;
import com.tailoris.marketing.entity.UserCoupon;
import com.tailoris.marketing.mapper.CouponTemplateMapper;
import com.tailoris.marketing.mapper.UserCouponMapper;
import com.tailoris.marketing.service.CouponService;
import com.tailoris.marketing.service.MktStatisticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 优惠券 Service 实现 - 增强版
 * 任务编号: MKT-001
 * 修复: 分布式锁使用、库存预扣减、有效期计算、过期扫描
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CouponServiceImpl implements CouponService {

    private final CouponTemplateMapper couponTemplateMapper;
    private final UserCouponMapper userCouponMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final DistributedLock distributedLock;
    private final MktStatisticsService statisticsService;

    private static final String COUPON_STOCK_KEY = "coupon:stock:";
    private static final String COUPON_USER_LIMIT_KEY = "coupon:userlimit:";
    private static final String COUPON_IDEMPOTENT_KEY = "coupon:idem:";
    private static final String COUPON_LOCK_KEY = "coupon:lock:";
    private static final long LOCK_TIMEOUT_MS = 5000;
    private static final int BATCH_SIZE = 500;

    @Override
    @Transactional
    public CouponTemplate createCoupon(CouponCreateRequest request) {
        validateCouponRequest(request);
        CouponTemplate template = new CouponTemplate();
        template.setId(SnowflakeIdGenerator.getInstance().nextId());
        template.setName(request.getName());
        template.setType(request.getType());
        template.setDiscountType(request.getDiscountType());
        template.setDiscountValue(request.getDiscountValue());
        template.setMinAmount(request.getMinAmount() != null ? request.getMinAmount() : BigDecimal.ZERO);
        template.setMaxDiscount(request.getMaxDiscount());
        template.setTotalCount(request.getTotalCount());
        template.setIssuedCount(0);
        template.setReceivedCount(0);
        template.setUsedCount(0);
        template.setPerLimit(request.getPerLimit() != null ? request.getPerLimit() : 1);
        template.setScopeType(request.getScopeType() != null ? request.getScopeType() : 1);
        template.setScopeValue(request.getScopeValue());
        template.setStartTime(request.getStartTime());
        template.setEndTime(request.getEndTime());
        template.setReceiveStartTime(request.getReceiveStartTime() != null ? request.getReceiveStartTime() : request.getStartTime());
        template.setReceiveEndTime(request.getReceiveEndTime() != null ? request.getReceiveEndTime() : request.getEndTime());
        template.setStatus(0);
        template.setDescription(request.getDescription());
        couponTemplateMapper.insert(template);
        log.info("创建优惠券: id={}, name={}", template.getId(), template.getName());
        return template;
    }

    @Override
    @Transactional
    public void receiveCoupon(Long userId, Long couponId) {
        // 幂等性保护：1秒内同一用户对同一券的重复请求直接拒绝
        String idemKey = COUPON_IDEMPOTENT_KEY + userId + ":" + couponId;
        Boolean firstReq = stringRedisTemplate.opsForValue()
                .setIfAbsent(idemKey, "1", 1, TimeUnit.SECONDS);
        if (Boolean.FALSE.equals(firstReq)) {
            throw new BusinessException("请求处理中，请勿重复提交");
        }

        CouponTemplate template = couponTemplateMapper.selectById(couponId);
        if (template == null) {
            throw new BusinessException("优惠券不存在");
        }
        LocalDateTime now = LocalDateTime.now();
        if (template.getStatus() != 1) {
            throw new BusinessException("优惠券不可领取");
        }
        if (template.getReceiveStartTime() != null && now.isBefore(template.getReceiveStartTime())) {
            throw new BusinessException("未到领取时间");
        }
        if (template.getReceiveEndTime() != null && now.isAfter(template.getReceiveEndTime())) {
            throw new BusinessException("已过领取时间");
        }

        // H-010: 原子性限领检查 — 使用 Lua 脚本避免 check-then-act 竞态
        if (template.getPerLimit() != null && template.getPerLimit() > 0) {
            String userLimitKey = COUPON_USER_LIMIT_KEY + couponId + ":" + userId;
            String luaScript =
                "local current = tonumber(redis.call('GET', KEYS[1]) or '0')\n" +
                "local limit = tonumber(ARGV[1])\n" +
                "if current >= limit then\n" +
                "    return 0\n" +
                "end\n" +
                "redis.call('INCR', KEYS[1])\n" +
                "redis.call('EXPIRE', KEYS[1], 2592000)\n" +
                "return 1\n";
            Long luaResult = stringRedisTemplate.execute(
                new org.springframework.data.redis.core.script.DefaultRedisScript<>(luaScript, Long.class),
                java.util.Collections.singletonList(userLimitKey),
                String.valueOf(template.getPerLimit()));
            if (luaResult == null || luaResult == 0) {
                throw new BusinessException("已达到领取上限");
            }
        }

        // 分布式锁：使用 tryLock + finally
        String lockKey = COUPON_LOCK_KEY + couponId;
        String lockToken = distributedLock.tryLock(lockKey, LOCK_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        if (lockToken == null) {
            throw new BusinessException("系统繁忙，请稍后重试");
        }
        try {
            // 双重检查库存
            CouponTemplate fresh = couponTemplateMapper.selectById(couponId);
            if (fresh.getTotalCount() > 0 && fresh.getReceivedCount() >= fresh.getTotalCount()) {
                throw new BusinessException("优惠券已领完");
            }

            // 持久化
            fresh.setReceivedCount(fresh.getReceivedCount() + 1);
            couponTemplateMapper.updateById(fresh);

            UserCoupon userCoupon = new UserCoupon();
            userCoupon.setId(SnowflakeIdGenerator.getInstance().nextId());
            userCoupon.setUserId(userId);
            userCoupon.setCouponId(couponId);
            userCoupon.setCouponCode("UC" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 6).toUpperCase());
            userCoupon.setStatus(0);
            userCoupon.setValidStartTime(template.getReceiveStartTime() != null ? template.getReceiveStartTime() : now);
            // 优先 daysAfterReceive
            if (template.getDaysAfterReceive() != null && template.getDaysAfterReceive() > 0) {
                userCoupon.setValidEndTime(now.plusDays(template.getDaysAfterReceive()));
            } else {
                userCoupon.setValidEndTime(template.getEndTime());
            }
            userCouponMapper.insert(userCoupon);
            log.info("用户领取优惠券: userId={}, couponId={}, code={}", userId, couponId, userCoupon.getCouponCode());

            // 营销统计
            statisticsService.recordParticipate(1, couponId, template.getName());
        } finally {
            distributedLock.unlock(lockKey, lockToken);
        }
    }

    @Override
    @Transactional
    public void useCoupon(Long userId, Long userCouponId, Long orderId) {
        UserCoupon userCoupon = userCouponMapper.selectById(userCouponId);
        if (userCoupon == null || !userCoupon.getUserId().equals(userId)) {
            throw new BusinessException("优惠券不存在");
        }
        if (userCoupon.getStatus() != 0) {
            throw new BusinessException("优惠券不可用");
        }
        if (userCoupon.getValidEndTime().isBefore(LocalDateTime.now())) {
            userCoupon.setStatus(2);
            userCouponMapper.updateById(userCoupon);
            throw new BusinessException("优惠券已过期");
        }

        userCoupon.setStatus(3);
        userCoupon.setOrderId(orderId);
        userCoupon.setOrderNo("ORD" + orderId);
        userCoupon.setUsedTime(LocalDateTime.now());
        userCouponMapper.updateById(userCoupon);

        // 更新模板已使用数
        CouponTemplate template = couponTemplateMapper.selectById(userCoupon.getCouponId());
        if (template != null) {
            template.setUsedCount(template.getUsedCount() == null ? 1 : template.getUsedCount() + 1);
            couponTemplateMapper.updateById(template);
            // 营销统计
            statisticsService.recordOrder(1, userCoupon.getCouponId(), template.getName(),
                    BigDecimal.ZERO, BigDecimal.ZERO);
        }
        log.info("使用优惠券: userId={}, userCouponId={}, orderId={}", userId, userCouponId, orderId);
    }

    @Override
    public PageResponse<UserCoupon> listCoupons(Long userId, PageRequest pageRequest, Integer status) {
        Page<UserCoupon> page = new Page<>(pageRequest.getPageNum(), pageRequest.getPageSize());
        LambdaQueryWrapper<UserCoupon> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserCoupon::getUserId, userId);
        if (status != null) {
            wrapper.eq(UserCoupon::getStatus, status);
        }
        wrapper.orderByDesc(UserCoupon::getCreateTime);
        Page<UserCoupon> result = userCouponMapper.selectPage(page, wrapper);
        return new PageResponse<>(result.getRecords(), result.getTotal(), pageRequest.getPageNum(), pageRequest.getPageSize());
    }

    @Override
    public List<UserCoupon> getAvailableCoupons(Long userId, BigDecimal orderAmount, Long shopId) {
        LambdaQueryWrapper<UserCoupon> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserCoupon::getUserId, userId)
                .eq(UserCoupon::getStatus, 0)
                .gt(UserCoupon::getValidEndTime, LocalDateTime.now());
        List<UserCoupon> userCoupons = userCouponMapper.selectList(wrapper);
        // M-001 修复: 批量查询模板，避免 N+1 问题
        if (orderAmount != null && !userCoupons.isEmpty()) {
            List<Long> couponIds = userCoupons.stream()
                    .map(UserCoupon::getCouponId)
                    .distinct()
                    .collect(Collectors.toList());
            Map<Long, CouponTemplate> templateMap = couponTemplateMapper.selectBatchIds(couponIds).stream()
                    .collect(Collectors.toMap(CouponTemplate::getId, t -> t, (a, b) -> a));
            return userCoupons.stream()
                    .filter(uc -> {
                        CouponTemplate t = templateMap.get(uc.getCouponId());
                        return t == null || t.getMinAmount() == null
                                || orderAmount.compareTo(t.getMinAmount()) >= 0;
                    })
                    .toList();
        }
        return userCoupons;
    }

    @Override
    public CouponTemplate getCouponTemplate(Long couponId) {
        return couponTemplateMapper.selectById(couponId);
    }

    /**
     * 定时任务：每分钟扫描过期优惠券（标记为已过期）
     */
    @Scheduled(fixedDelay = 60_000, initialDelay = 60_000)
    public void scanExpiredCoupons() {
        try {
            int total = 0;
            int affected = 0;
            while (true) {
                LambdaQueryWrapper<UserCoupon> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(UserCoupon::getStatus, 0)
                        .lt(UserCoupon::getValidEndTime, LocalDateTime.now())
                        .last("LIMIT " + BATCH_SIZE);
                List<UserCoupon> expired = userCouponMapper.selectList(wrapper);
                if (expired.isEmpty()) {
                    break;
                }
                // M-001 修复: 使用批量更新代替逐条 updateById
                for (UserCoupon uc : expired) {
                    uc.setStatus(2);
                }
                userCouponMapper.updateBatch(expired);
                affected += expired.size();
                total++;
                if (expired.size() < BATCH_SIZE) {
                    break;
                }
            }
            if (affected > 0) {
                log.info("扫描过期优惠券完成: 共处理 {} 张", affected);
            }
        } catch (Exception e) {
            log.error("扫描过期优惠券异常", e);
        }
    }

    /**
     * 定时任务：每5分钟更新优惠券状态（未开始→进行中→已结束）
     */
    @Scheduled(fixedDelay = 300_000, initialDelay = 120_000)
    public void updateCouponStatus() {
        try {
            LocalDateTime now = LocalDateTime.now();
            // 进行中
            CouponTemplate ongoing = new CouponTemplate();
            ongoing.setStatus(1);
            couponTemplateMapper.update(ongoing, new LambdaQueryWrapper<CouponTemplate>()
                    .eq(CouponTemplate::getStatus, 0)
                    .le(CouponTemplate::getStartTime, now)
                    .gt(CouponTemplate::getEndTime, now));
            // 已结束
            CouponTemplate ended = new CouponTemplate();
            ended.setStatus(2);
            couponTemplateMapper.update(ended, new LambdaQueryWrapper<CouponTemplate>()
                    .lt(CouponTemplate::getEndTime, now)
                    .ne(CouponTemplate::getStatus, 3));
            log.debug("更新优惠券状态完成");
        } catch (Exception e) {
            log.error("更新优惠券状态异常", e);
        }
    }

    private void validateCouponRequest(CouponCreateRequest request) {
        if (request.getName() == null || request.getName().isEmpty()) {
            throw new BusinessException("优惠券名称不能为空");
        }
        if (request.getType() == null) {
            throw new BusinessException("优惠券类型不能为空");
        }
        if (request.getDiscountValue() == null || request.getDiscountValue().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("优惠值必须大于0");
        }
        if (request.getStartTime() == null || request.getEndTime() == null) {
            throw new BusinessException("有效期不能为空");
        }
        if (request.getEndTime().isBefore(request.getStartTime())) {
            throw new BusinessException("结束时间不能早于开始时间");
        }
        if (request.getTotalCount() != null && request.getTotalCount() < -1) {
            throw new BusinessException("发放总量不合法");
        }
    }
}
