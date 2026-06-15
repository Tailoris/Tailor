package com.tailoris.merchant.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.tailoris.common.annotation.RateLimit;
import com.tailoris.common.result.Result;
import com.tailoris.merchant.entity.MerchantDashboardStats;
import com.tailoris.merchant.service.IMerchantDashboardStatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Merchant dashboard controller - MER-005.
 *
 * <p>商家数据工作台控制器，提供流量/转化/交易/收益多维统计。</p>
 *
 * @author Tailor IS Team
 * @since 2026-06-03
 */
@Tag(name = "Merchant Dashboard API", description = "商家数据看板接口 - MER-005")
@RestController
@RequestMapping("/api/v1/merchant/dashboard")
@RequiredArgsConstructor
@Validated
public class MerchantDashboardController {

    private final IMerchantDashboardStatsService dashboardStatsService;

    @Operation(summary = "获取工作台汇总数据（全店）")
    @GetMapping("/summary")
    @SaCheckLogin
    @RateLimit(key = "merchant_dashboard", permitsPerSecond = 3)
    public Result<Map<String, Object>> getSummary(
            @RequestHeader(value = "X-Merchant-Id", required = false) Long merchantId) {
        if (merchantId == null) {
            return Result.fail("商家未登录");
        }
        return Result.success(dashboardStatsService.getDashboardSummary(merchantId));
    }

    @Operation(summary = "获取工作台汇总数据（指定店铺）")
    @GetMapping("/summary/shop/{shopId}")
    @SaCheckLogin
    @RateLimit(key = "merchant_dashboard_shop", permitsPerSecond = 3)
    public Result<Map<String, Object>> getSummaryByShop(
            @RequestHeader(value = "X-Merchant-Id", required = false) Long merchantId,
            @Parameter(description = "店铺ID") @PathVariable Long shopId) {
        if (merchantId == null) {
            return Result.fail("商家未登录");
        }
        return Result.success(dashboardStatsService.getDashboardSummary(merchantId, shopId));
    }

    @Operation(summary = "获取今日实时数据（全店）")
    @GetMapping("/today")
    @SaCheckLogin
    public Result<MerchantDashboardStats> getTodayStats(
            @RequestHeader(value = "X-Merchant-Id", required = false) Long merchantId) {
        if (merchantId == null) {
            return Result.fail("商家未登录");
        }
        return Result.success(dashboardStatsService.getTodayStats(merchantId));
    }

    @Operation(summary = "获取今日实时数据（指定店铺）")
    @GetMapping("/today/shop/{shopId}")
    @SaCheckLogin
    public Result<MerchantDashboardStats> getTodayStatsByShop(
            @RequestHeader(value = "X-Merchant-Id", required = false) Long merchantId,
            @Parameter(description = "店铺ID") @PathVariable Long shopId) {
        if (merchantId == null) {
            return Result.fail("商家未登录");
        }
        return Result.success(dashboardStatsService.getTodayStats(merchantId, shopId));
    }

    @Operation(summary = "按日期范围查询全店统计数据")
    @GetMapping("/range")
    @SaCheckLogin
    public Result<List<MerchantDashboardStats>> getStatsByRange(
            @RequestHeader(value = "X-Merchant-Id", required = false) Long merchantId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        if (merchantId == null) {
            return Result.fail("商家未登录");
        }
        return Result.success(dashboardStatsService.getStatsByDateRange(
                merchantId, startDate.toString(), endDate.toString()));
    }

    @Operation(summary = "按日期范围查询指定店铺统计数据")
    @GetMapping("/range/shop/{shopId}")
    @SaCheckLogin
    public Result<List<MerchantDashboardStats>> getShopStatsByRange(
            @RequestHeader(value = "X-Merchant-Id", required = false) Long merchantId,
            @Parameter(description = "店铺ID") @PathVariable Long shopId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        if (merchantId == null) {
            return Result.fail("商家未登录");
        }
        return Result.success(dashboardStatsService.getShopStatsByDateRange(
                merchantId, shopId, startDate.toString(), endDate.toString()));
    }

    @Operation(summary = "获取最近N天趋势数据")
    @GetMapping("/trend")
    @SaCheckLogin
    public Result<List<MerchantDashboardStats>> getTrend(
            @RequestHeader(value = "X-Merchant-Id", required = false) Long merchantId,
            @Parameter(description = "店铺ID，可为空") @RequestParam(required = false) Long shopId,
            @Parameter(description = "天数（默认30，最大90）") @RequestParam(defaultValue = "30") int days) {
        if (merchantId == null) {
            return Result.fail("商家未登录");
        }
        int maxDays = 90;
        int realDays = Math.min(Math.max(days, 1), maxDays);
        return Result.success(dashboardStatsService.getTrendData(merchantId, shopId, realDays));
    }

    @Operation(summary = "刷新今日实时数据")
    @PostMapping("/refresh")
    @SaCheckLogin
    @RateLimit(key = "merchant_dashboard_refresh", permitsPerSecond = 1)
    public Result<MerchantDashboardStats> refresh(
            @RequestHeader(value = "X-Merchant-Id", required = false) Long merchantId) {
        if (merchantId == null) {
            return Result.fail("商家未登录");
        }
        return Result.success(dashboardStatsService.refreshTodayStats(merchantId));
    }
}
