package com.tailoris.marketing.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tailoris.common.dto.PageRequest;
import com.tailoris.common.dto.PageResponse;
import com.tailoris.common.exception.BusinessException;
import com.tailoris.common.util.SnowflakeIdGenerator;
import com.tailoris.marketing.dto.SeckillCreateRequest;
import com.tailoris.marketing.entity.SeckillActivity;
import com.tailoris.marketing.entity.SeckillProduct;
import com.tailoris.marketing.mapper.SeckillActivityMapper;
import com.tailoris.marketing.mapper.SeckillProductMapper;
import com.tailoris.marketing.service.SeckillService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 秒杀 Service 实现 - 增强版
 * 任务编号: MKT-003
 * 修复: Redis 原子 Lua 预扣减、限购、库存同步
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SeckillServiceImpl implements SeckillService {

    private final SeckillActivityMapper seckillActivityMapper;
    private final SeckillProductMapper seckillProductMapper;
    private final StringRedisTemplate stringRedisTemplate;

    private static final String SECKILL_STOCK_KEY = "seckill:stock:";
    private static final String SECKILL_LIMIT_KEY = "seckill:limit:";
    private static final String SECKILL_USER_ORDER_KEY = "seckill:order:";

    /**
     * 原子预扣减 Lua 脚本
     * KEYS[1] = 库存key, KEYS[2] = 限购key
     * ARGV[1] = 限购数量
     * 返回值：1=成功 0=库存不足 -1=已达限购
     */
    private static final String SECKILL_LUA_SCRIPT =
            "local stock = tonumber(redis.call('GET', KEYS[1]))\n" +
            "if not stock or stock <= 0 then return 0 end\n" +
            "local limit = tonumber(redis.call('GET', KEYS[2]) or '0')\n" +
            "local maxLimit = tonumber(ARGV[1])\n" +
            "if limit >= maxLimit then return -1 end\n" +
            "redis.call('DECR', KEYS[1])\n" +
            "redis.call('INCR', KEYS[2])\n" +
            "redis.call('EXPIRE', KEYS[2], 604800)\n" +
            "return 1";

    private final RedisScript<Long> seckillScript = new DefaultRedisScript<>(SECKILL_LUA_SCRIPT, Long.class);

    @Override
    @Transactional
    public SeckillActivity createActivity(SeckillCreateRequest request) {
        SeckillActivity activity = new SeckillActivity();
        activity.setId(SnowflakeIdGenerator.getInstance().nextId());
        activity.setName(request.getName());
        activity.setStartTime(request.getStartTime());
        activity.setEndTime(request.getEndTime());
        activity.setStatus(0);
        activity.setDescription(request.getDescription());
        activity.setSort(request.getSort() != null ? request.getSort() : 0);
        activity.setProductCount(0);
        seckillActivityMapper.insert(activity);

        SeckillProduct product = new SeckillProduct();
        product.setId(SnowflakeIdGenerator.getInstance().nextId());
        product.setActivityId(activity.getId());
        product.setProductId(request.getProductId());
        product.setSkuId(request.getSkuId());
        product.setSeckillPrice(request.getSeckillPrice());
        product.setOriginalPrice(request.getOriginalPrice());
        product.setStock(request.getStock());
        product.setAvailableStock(request.getStock());
        product.setLimitCount(request.getLimitCount() != null ? request.getLimitCount() : 1);
        product.setSort(request.getSort() != null ? request.getSort() : 0);
        product.setStatus(1);
        product.setOrderCount(0);
        seckillProductMapper.insert(product);

        activity.setProductCount(1);
        seckillActivityMapper.updateById(activity);

        // 预热库存到 Redis
        try {
            stringRedisTemplate.opsForValue().set(
                    SECKILL_STOCK_KEY + product.getId(),
                    String.valueOf(request.getStock()), 7, TimeUnit.DAYS);
        } catch (Exception e) {
            log.warn("预热秒杀库存到Redis失败: {}", e.getMessage());
        }
        log.info("创建秒杀活动: activityId={}, productId={}", activity.getId(), product.getId());
        return activity;
    }

    @Override
    public void joinSeckill(Long userId, Long seckillProductId) {
        SeckillProduct product = seckillProductMapper.selectById(seckillProductId);
        if (product == null || product.getStatus() != 1) {
            throw new BusinessException("秒杀商品不存在或已下架");
        }
        SeckillActivity activity = seckillActivityMapper.selectById(product.getActivityId());
        if (activity == null) {
            throw new BusinessException("秒杀活动不存在");
        }
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(activity.getStartTime()) || now.isAfter(activity.getEndTime())) {
            throw new BusinessException("秒杀活动未开始或已结束");
        }

        String stockKey = SECKILL_STOCK_KEY + seckillProductId;
        String limitKey = SECKILL_LIMIT_KEY + seckillProductId + ":" + userId;

        // 原子预扣减库存
        Long result;
        try {
            result = stringRedisTemplate.execute(seckillScript,
                    Arrays.asList(stockKey, limitKey),
                    String.valueOf(product.getLimitCount() == null ? 1 : product.getLimitCount()));
        } catch (Exception e) {
            log.error("执行秒杀Lua脚本异常", e);
            throw new BusinessException("系统繁忙，请重试");
        }

        if (result == null) {
            throw new BusinessException("系统繁忙，请重试");
        }
        if (result == 0L) {
            throw new BusinessException("秒杀商品已售罄");
        }
        if (result == -1L) {
            throw new BusinessException("已达到购买上限");
        }

        // 异步同步DB（这里用同步更新以保证后续下单可见）
        try {
            product.setAvailableStock(product.getAvailableStock() == null ? 0 : product.getAvailableStock() - 1);
            product.setOrderCount(product.getOrderCount() == null ? 1 : product.getOrderCount() + 1);
            seckillProductMapper.updateById(product);
        } catch (Exception e) {
            log.error("更新DB库存失败，需要人工对账: seckillProductId={}", seckillProductId, e);
            // 不抛异常，保证用户体验（DB 异步对账）
        }
        log.info("秒杀下单成功: userId={}, seckillProductId={}", userId, seckillProductId);
    }

    @Override
    @Transactional
    public void cancelSeckill(Long userId, Long seckillProductId) {
        String stockKey = SECKILL_STOCK_KEY + seckillProductId;
        String limitKey = SECKILL_LIMIT_KEY + seckillProductId + ":" + userId;
        try {
            stringRedisTemplate.opsForValue().increment(stockKey, 1);
            Long limit = stringRedisTemplate.opsForValue().decrement(limitKey);
            if (limit != null && limit <= 0) {
                stringRedisTemplate.delete(limitKey);
            }
        } catch (Exception e) {
            log.warn("恢复秒杀库存失败: {}", e.getMessage());
        }
        // 恢复 DB 库存
        try {
            SeckillProduct product = seckillProductMapper.selectById(seckillProductId);
            if (product != null) {
                product.setAvailableStock(product.getAvailableStock() == null ? 1 : product.getAvailableStock() + 1);
                product.setOrderCount(Math.max(0, product.getOrderCount() == null ? 0 : product.getOrderCount() - 1));
                seckillProductMapper.updateById(product);
            }
        } catch (Exception e) {
            log.warn("恢复DB库存失败: {}", e.getMessage());
        }
    }

    @Override
    public PageResponse<SeckillProduct> listSeckillProducts(PageRequest pageRequest, Long activityId) {
        Page<SeckillProduct> page = new Page<>(pageRequest.getPageNum(), pageRequest.getPageSize());
        LambdaQueryWrapper<SeckillProduct> wrapper = new LambdaQueryWrapper<>();
        if (activityId != null) {
            wrapper.eq(SeckillProduct::getActivityId, activityId);
        }
        wrapper.eq(SeckillProduct::getStatus, 1);
        wrapper.orderByAsc(SeckillProduct::getSort);
        Page<SeckillProduct> result = seckillProductMapper.selectPage(page, wrapper);
        return new PageResponse<>(result.getRecords(), result.getTotal(),
                pageRequest.getPageNum(), pageRequest.getPageSize());
    }

    @Override
    public List<SeckillActivity> listActiveActivities() {
        LambdaQueryWrapper<SeckillActivity> wrapper = new LambdaQueryWrapper<>();
        wrapper.le(SeckillActivity::getStartTime, LocalDateTime.now())
                .ge(SeckillActivity::getEndTime, LocalDateTime.now())
                .eq(SeckillActivity::getStatus, 1)
                .orderByDesc(SeckillActivity::getSort);
        return seckillActivityMapper.selectList(wrapper);
    }

    @Override
    public SeckillProduct getSeckillProduct(Long seckillProductId) {
        return seckillProductMapper.selectById(seckillProductId);
    }
}
