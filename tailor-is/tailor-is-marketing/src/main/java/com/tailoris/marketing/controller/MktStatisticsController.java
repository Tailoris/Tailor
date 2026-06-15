package com.tailoris.marketing.controller;

import com.tailoris.common.result.Result;
import com.tailoris.marketing.entity.MktOrderPromotion;
import com.tailoris.marketing.entity.MktPromotionStats;
import com.tailoris.marketing.service.MktStatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 营销报表 Controller
 * 任务编号: MKT-008
 */
@Tag(name = "营销统计", description = "营销活动统计报表数据记录与查询")
@Slf4j
@RestController
@RequestMapping("/api/marketing/stats")
@RequiredArgsConstructor
public class MktStatisticsController {

    private final MktStatisticsService statisticsService;

    /** 记录曝光 */
    @Operation(summary = "记录活动曝光", description = "记录营销活动的曝光次数")
    @PostMapping("/exposure")
    public Result<Void> recordExposure(
            @RequestParam Integer promotionType,
            @RequestParam Long promotionId,
            @RequestParam(required = false) String promotionName) {
        statisticsService.recordExposure(promotionType, promotionId, promotionName);
        return Result.success();
    }

    /** 记录点击 */
    @Operation(summary = "记录活动点击", description = "记录营销活动的点击次数")
    @PostMapping("/click")
    public Result<Void> recordClick(
            @RequestParam Integer promotionType,
            @RequestParam Long promotionId,
            @RequestParam(required = false) String promotionName) {
        statisticsService.recordClick(promotionType, promotionId, promotionName);
        return Result.success();
    }

    /** 记录参与 */
    @Operation(summary = "记录活动参与", description = "记录营销活动的参与次数")
    @PostMapping("/participate")
    public Result<Void> recordParticipate(
            @RequestParam Integer promotionType,
            @RequestParam Long promotionId,
            @RequestParam(required = false) String promotionName) {
        statisticsService.recordParticipate(promotionType, promotionId, promotionName);
        return Result.success();
    }

    /** 记录订单 */
    @Operation(summary = "记录订单营销转化", description = "记录营销活动带来的订单转化数据")
    @PostMapping("/order")
    public Result<Void> recordOrder(
            @RequestParam Integer promotionType,
            @RequestParam Long promotionId,
            @RequestParam(required = false) String promotionName,
            @RequestParam BigDecimal orderAmount,
            @RequestParam BigDecimal discountAmount) {
        statisticsService.recordOrder(promotionType, promotionId, promotionName, orderAmount, discountAmount);
        return Result.success();
    }

    /** 活动最近N天统计 */
    @Operation(summary = "查询活动统计", description = "查询指定活动最近N天的统计数据")
    @GetMapping("/promotion")
    public Result<List<MktPromotionStats>> getPromotionStats(
            @RequestParam Integer promotionType,
            @RequestParam Long promotionId,
            @RequestParam(defaultValue = "30") Integer days) {
        return Result.success(statisticsService.getPromotionStats(promotionType, promotionId, days));
    }

    /** 营销大盘 */
    @Operation(summary = "查询营销大盘", description = "查询指定时间范围内的营销整体数据概览")
    @GetMapping("/overview")
    public Result<List<Map<String, Object>>> getMarketingOverview(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return Result.success(statisticsService.getMarketingOverview(startDate, endDate));
    }

    /** Top 活动 */
    @Operation(summary = "查询热门活动排行", description = "查询指定时间范围内效果最好的活动排行")
    @GetMapping("/top")
    public Result<List<MktPromotionStats>> getTopPromotions(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "10") Integer limit) {
        return Result.success(statisticsService.getTopPromotions(startDate, endDate, limit));
    }

    /** 更新 ROI */
    @Operation(summary = "更新活动ROI", description = "计算并更新营销活动的投入产出比")
    @PostMapping("/roi")
    public Result<Void> updateRoi(
            @RequestParam Integer promotionType,
            @RequestParam Long promotionId,
            @RequestParam BigDecimal cost) {
        statisticsService.updateRoi(promotionType, promotionId, cost);
        return Result.success();
    }

    /** 记录订单营销关联 */
    @Operation(summary = "记录订单营销关联", description = "记录订单与营销活动的关联关系")
    @PostMapping("/order-promotion")
    public Result<MktOrderPromotion> recordOrderPromotion(
            @RequestParam Long orderId,
            @RequestParam Integer promotionType,
            @RequestParam Long promotionId,
            @RequestParam(required = false) String promotionName,
            @RequestParam(required = false) BigDecimal discountAmount,
            @RequestParam(required = false) Long couponId,
            @RequestParam(required = false) Long groupInstanceId,
            @RequestParam(required = false) Long seckillId,
            @RequestParam(required = false) Integer pointsUsed) {
        return Result.success(statisticsService.recordOrderPromotion(
                orderId, promotionType, promotionId, promotionName, discountAmount,
                couponId, groupInstanceId, seckillId, pointsUsed));
    }

    /** 订单营销记录 */
    @Operation(summary = "查询订单营销记录", description = "查询指定订单关联的营销活动记录")
    @GetMapping("/order-promotion/{orderId}")
    public Result<List<MktOrderPromotion>> getOrderPromotions(@PathVariable Long orderId) {
        return Result.success(statisticsService.getOrderPromotions(orderId));
    }
}
