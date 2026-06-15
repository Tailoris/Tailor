package com.tailoris.marketing.service;

import com.tailoris.marketing.entity.MktPromotionStats;
import com.tailoris.marketing.entity.MktOrderPromotion;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 营销报表 Service
 * 任务编号: MKT-008
 */
public interface MktStatisticsService {

    /**
     * 记录曝光
     */
    void recordExposure(Integer promotionType, Long promotionId, String promotionName);

    /**
     * 记录点击
     */
    void recordClick(Integer promotionType, Long promotionId, String promotionName);

    /**
     * 记录参与
     */
    void recordParticipate(Integer promotionType, Long promotionId, String promotionName);

    /**
     * 记录订单
     */
    void recordOrder(Integer promotionType, Long promotionId, String promotionName,
                     BigDecimal orderAmount, BigDecimal discountAmount);

    /**
     * 查询活动的最近N天统计
     */
    List<MktPromotionStats> getPromotionStats(Integer promotionType, Long promotionId, int days);

    /**
     * 营销大盘（按类型聚合）
     */
    List<Map<String, Object>> getMarketingOverview(LocalDate startDate, LocalDate endDate);

    /**
     * Top N 活动（按订单金额）
     */
    List<MktPromotionStats> getTopPromotions(LocalDate startDate, LocalDate endDate, int limit);

    /**
     * 计算并更新 ROI
     */
    void updateRoi(Integer promotionType, Long promotionId, BigDecimal cost);

    /**
     * 记录订单营销关联
     */
    MktOrderPromotion recordOrderPromotion(Long orderId, Integer promotionType, Long promotionId,
                                           String promotionName, BigDecimal discountAmount,
                                           Long couponId, Long groupInstanceId, Long seckillId, Integer pointsUsed);

    /**
     * 查询订单营销记录
     */
    List<MktOrderPromotion> getOrderPromotions(Long orderId);
}
