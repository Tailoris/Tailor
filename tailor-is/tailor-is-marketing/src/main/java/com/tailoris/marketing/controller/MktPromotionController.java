package com.tailoris.marketing.controller;

import com.tailoris.common.dto.PageRequest;
import com.tailoris.common.dto.PageResponse;
import com.tailoris.common.result.Result;
import com.tailoris.marketing.entity.MktPromotionRule;
import com.tailoris.marketing.entity.MktPromotionStep;
import com.tailoris.marketing.service.MktPromotionService;
import com.tailoris.marketing.service.MktPromotionService.PromotionDiscountResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * 阶梯满减满赠 Controller
 * 任务编号: MKT-004
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/marketing/promotion")
@RequiredArgsConstructor
public class MktPromotionController {

    private final MktPromotionService promotionService;

    /** 创建促销 */
    @Operation(summary = "创建促销", description = "创建阶梯满减满赠促销活动")
    @PostMapping
    public Result<MktPromotionRule> createPromotion(
            @RequestBody PromotionCreateRequest request) {
        return Result.success(promotionService.createPromotion(
                request.getRule(), request.getSteps()));
    }

    /** 更新促销 */
    @Operation(summary = "更新促销", description = "更新阶梯满减满赠促销活动")
    @PutMapping("/{id}")
    public Result<MktPromotionRule> updatePromotion(
            @PathVariable Long id,
            @RequestBody PromotionCreateRequest request) {
        request.getRule().setId(id);
        return Result.success(promotionService.updatePromotion(
                request.getRule(), request.getSteps()));
    }

    /** 取消促销 */
    @Operation(summary = "取消促销", description = "取消促销活动")
    @PostMapping("/{id}/cancel")
    public Result<Void> cancelPromotion(@PathVariable Long id) {
        promotionService.cancelPromotion(id);
        return Result.success();
    }

    /** 促销详情 */
    @Operation(summary = "促销详情", description = "获取促销活动详情")
    @GetMapping("/{id}")
    public Result<MktPromotionRule> getPromotionDetail(@PathVariable Long id) {
        return Result.success(promotionService.getPromotionDetail(id));
    }

    /** 促销列表 */
    @Operation(summary = "促销列表", description = "分页查询促销活动列表")
    @GetMapping("/list")
    public Result<PageResponse<MktPromotionRule>> listPromotions(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) Long shopId,
            @RequestParam(required = false) Integer status) {
        return Result.success(promotionService.listPromotions(
                new PageRequest(pageNum, pageSize), shopId, status));
    }

    /** 生效中的促销 */
    @Operation(summary = "生效中的促销", description = "获取当前生效中的促销活动列表")
    @GetMapping("/active")
    public Result<List<MktPromotionRule>> listActiveRules(
            @RequestParam(required = false) Long shopId) {
        return Result.success(promotionService.listActiveRules(shopId));
    }

    /** 计算最优优惠 */
    @Operation(summary = "计算最优优惠", description = "计算促销活动的最优优惠方案")
    @PostMapping("/calculate")
    public Result<PromotionDiscountResult> calculate(
            @RequestParam(required = false) Long shopId,
            @RequestParam BigDecimal totalAmount,
            @RequestParam(defaultValue = "0") Integer itemCount) {
        return Result.success(promotionService.calculateOptimalDiscount(shopId, totalAmount, itemCount));
    }

    /** 创建促销请求体 */
    @lombok.Data
    public static class PromotionCreateRequest {
        private MktPromotionRule rule;
        private List<MktPromotionStep> steps;
    }
}
