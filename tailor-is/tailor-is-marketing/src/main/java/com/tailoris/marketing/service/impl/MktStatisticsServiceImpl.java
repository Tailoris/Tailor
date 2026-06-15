package com.tailoris.marketing.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tailoris.common.util.SnowflakeIdGenerator;
import com.tailoris.marketing.entity.MktOrderPromotion;
import com.tailoris.marketing.entity.MktPromotionStats;
import com.tailoris.marketing.mapper.MktOrderPromotionMapper;
import com.tailoris.marketing.mapper.MktPromotionStatsMapper;
import com.tailoris.marketing.service.MktStatisticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 营销报表 Service 实现
 * 任务编号: MKT-008
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MktStatisticsServiceImpl implements MktStatisticsService {

    private final MktPromotionStatsMapper statsMapper;
    private final MktOrderPromotionMapper orderPromotionMapper;
    private final StringRedisTemplate stringRedisTemplate;

    private static final String EXPOSURE_KEY_PREFIX = "mkt:stat:exposure:";
    private static final String CLICK_KEY_PREFIX = "mkt:stat:click:";
    private static final String PARTICIPATE_KEY_PREFIX = "mkt:stat:participate:";

    @Override
    public void recordExposure(Integer promotionType, Long promotionId, String promotionName) {
        try {
            String key = EXPOSURE_KEY_PREFIX + promotionType + ":" + promotionId;
            stringRedisTemplate.opsForValue().increment(key);
            stringRedisTemplate.expire(key, 7, TimeUnit.DAYS);
        } catch (Exception e) {
            log.warn("记录曝光失败: type={}, id={}", promotionType, promotionId, e);
        }
    }

    @Override
    public void recordClick(Integer promotionType, Long promotionId, String promotionName) {
        try {
            String key = CLICK_KEY_PREFIX + promotionType + ":" + promotionId;
            stringRedisTemplate.opsForValue().increment(key);
            stringRedisTemplate.expire(key, 7, TimeUnit.DAYS);
        } catch (Exception e) {
            log.warn("记录点击失败: type={}, id={}", promotionType, promotionId, e);
        }
    }

    @Override
    public void recordParticipate(Integer promotionType, Long promotionId, String promotionName) {
        try {
            String key = PARTICIPATE_KEY_PREFIX + promotionType + ":" + promotionId;
            stringRedisTemplate.opsForValue().increment(key);
            stringRedisTemplate.expire(key, 7, TimeUnit.DAYS);
        } catch (Exception e) {
            log.warn("记录参与失败: type={}, id={}", promotionType, promotionId, e);
        }
    }

    @Override
    @Transactional
    public void recordOrder(Integer promotionType, Long promotionId, String promotionName,
                            BigDecimal orderAmount, BigDecimal discountAmount) {
        LocalDate today = LocalDate.now();
        // 尝试获取或插入今日统计
        MktPromotionStats stats = statsMapper.selectOne(new LambdaQueryWrapper<MktPromotionStats>()
                .eq(MktPromotionStats::getPromotionType, promotionType)
                .eq(MktPromotionStats::getPromotionId, promotionId)
                .eq(MktPromotionStats::getStatDate, today));
        if (stats == null) {
            stats = new MktPromotionStats();
            stats.setId(SnowflakeIdGenerator.getInstance().nextId());
            stats.setPromotionType(promotionType);
            stats.setPromotionId(promotionId);
            stats.setPromotionName(promotionName);
            stats.setStatDate(today);
            stats.setExposureCount(0L);
            stats.setClickCount(0L);
            stats.setParticipateCount(0L);
            stats.setOrderCount(0L);
            stats.setOrderAmount(BigDecimal.ZERO);
            stats.setDiscountAmount(BigDecimal.ZERO);
            stats.setRoi(BigDecimal.ZERO);
            statsMapper.insert(stats);
        }
        // 从 Redis 同步曝光/点击/参与数
        syncRedisToDb(stats);

        stats.setOrderCount(stats.getOrderCount() == null ? 1 : stats.getOrderCount() + 1);
        stats.setOrderAmount((stats.getOrderAmount() == null ? BigDecimal.ZERO : stats.getOrderAmount())
                .add(orderAmount == null ? BigDecimal.ZERO : orderAmount));
        stats.setDiscountAmount((stats.getDiscountAmount() == null ? BigDecimal.ZERO : stats.getDiscountAmount())
                .add(discountAmount == null ? BigDecimal.ZERO : discountAmount));
        statsMapper.updateById(stats);
        log.debug("记录订单统计: type={}, id={}, amount={}", promotionType, promotionId, orderAmount);
    }

    @Override
    public List<MktPromotionStats> getPromotionStats(Integer promotionType, Long promotionId, int days) {
        return statsMapper.selectByPromotionRecent(promotionType, promotionId, days);
    }

    @Override
    public List<Map<String, Object>> getMarketingOverview(LocalDate startDate, LocalDate endDate) {
        List<Map<String, Object>> raw = statsMapper.aggregateByType(startDate, endDate);
        // 转换为标准格式
        List<Map<String, Object>> result = new java.util.ArrayList<>();
        for (Map<String, Object> item : raw) {
            Map<String, Object> m = new HashMap<>();
            m.put("promotionType", item.get("promotion_type"));
            m.put("orderCount", item.get("orderCount"));
            m.put("orderAmount", item.get("orderAmount"));
            m.put("discountAmount", item.get("discountAmount"));
            m.put("typeName", typeName((Integer) item.get("promotion_type")));
            result.add(m);
        }
        return result;
    }

    @Override
    public List<MktPromotionStats> getTopPromotions(LocalDate startDate, LocalDate endDate, int limit) {
        return statsMapper.selectTopByDateRange(startDate, endDate, limit);
    }

    @Override
    @Transactional
    public void updateRoi(Integer promotionType, Long promotionId, BigDecimal cost) {
        if (cost == null || cost.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        // 计算累计 ROI: ROI = 订单金额 / 投入成本
        List<MktPromotionStats> all = statsMapper.selectByPromotionRecent(promotionType, promotionId, 365);
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (MktPromotionStats s : all) {
            if (s.getOrderAmount() != null) {
                totalAmount = totalAmount.add(s.getOrderAmount());
            }
        }
        BigDecimal roi = totalAmount.divide(cost, 4, RoundingMode.HALF_UP);

        // 更新最近一条
        if (!all.isEmpty()) {
            MktPromotionStats latest = all.get(0);
            latest.setRoi(roi);
            statsMapper.updateById(latest);
        }
    }

    @Override
    @Transactional
    public MktOrderPromotion recordOrderPromotion(Long orderId, Integer promotionType, Long promotionId,
                                                  String promotionName, BigDecimal discountAmount,
                                                  Long couponId, Long groupInstanceId, Long seckillId,
                                                  Integer pointsUsed) {
        MktOrderPromotion op = new MktOrderPromotion();
        op.setId(SnowflakeIdGenerator.getInstance().nextId());
        op.setOrderId(orderId);
        op.setPromotionType(promotionType);
        op.setPromotionId(promotionId);
        op.setPromotionName(promotionName);
        op.setDiscountAmount(discountAmount == null ? BigDecimal.ZERO : discountAmount);
        op.setCouponId(couponId);
        op.setGroupInstanceId(groupInstanceId);
        op.setSeckillId(seckillId);
        op.setPointsUsed(pointsUsed == null ? 0 : pointsUsed);
        orderPromotionMapper.insert(op);

        // 同步记录订单统计
        recordOrder(promotionType, promotionId, promotionName, discountAmount, discountAmount);
        return op;
    }

    @Override
    public List<MktOrderPromotion> getOrderPromotions(Long orderId) {
        return orderPromotionMapper.selectByOrderId(orderId);
    }

    private void syncRedisToDb(MktPromotionStats stats) {
        try {
            String key;
            key = EXPOSURE_KEY_PREFIX + stats.getPromotionType() + ":" + stats.getPromotionId();
            String exp = stringRedisTemplate.opsForValue().get(key);
            if (exp != null) {
                stats.setExposureCount(Long.parseLong(exp));
            }
            key = CLICK_KEY_PREFIX + stats.getPromotionType() + ":" + stats.getPromotionId();
            String clk = stringRedisTemplate.opsForValue().get(key);
            if (clk != null) {
                stats.setClickCount(Long.parseLong(clk));
            }
            key = PARTICIPATE_KEY_PREFIX + stats.getPromotionType() + ":" + stats.getPromotionId();
            String par = stringRedisTemplate.opsForValue().get(key);
            if (par != null) {
                stats.setParticipateCount(Long.parseLong(par));
            }
        } catch (Exception e) {
            log.warn("同步Redis统计数据失败", e);
        }
    }

    private String typeName(Integer type) {
        if (type == null) return "未知";
        return switch (type) {
            case 1 -> "优惠券";
            case 2 -> "拼团";
            case 3 -> "秒杀";
            case 4 -> "满减满赠";
            case 5 -> "积分";
            default -> "其他";
        };
    }
}
