package com.tailoris.marketing.controller;

import com.tailoris.common.result.Result;
import com.tailoris.marketing.service.MktOrderMatchService;
import com.tailoris.marketing.service.MktOrderMatchService.OrderDiscountPlan;
import com.tailoris.marketing.service.MktOrderMatchService.OrderItemInput;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * 营销订单联动 Controller
 * 任务编号: MKT-007
 */
@Slf4j
@RestController
@RequestMapping("/api/marketing/order-match")
@RequiredArgsConstructor
@Tag(name = "订单营销联动", description = "MKT-007 下单自动匹配最优优惠")
public class MktOrderMatchController {

    private final MktOrderMatchService orderMatchService;

    /**
     * 计算订单最优优惠方案
     */
    @PostMapping("/calculate")
    public Result<OrderDiscountPlan> calculate(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestBody CalculateRequest request) {
        OrderDiscountPlan plan = orderMatchService.calculateOptimal(
                userId, request.getItems(), request.getTotalAmount());
        return Result.success(plan);
    }

    /**
     * 仅优惠券最优
     */
    @GetMapping("/coupon-only")
    public Result<OrderDiscountPlan> couponOnly(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestParam BigDecimal orderAmount) {
        return Result.success(orderMatchService.calculateCouponOnly(userId, orderAmount));
    }

    /** 计算请求体 */
    @lombok.Data
    public static class CalculateRequest {
        private List<OrderItemInput> items;
        private BigDecimal totalAmount;
    }
}
