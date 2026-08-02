package com.tailoris.admin.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.tailoris.api.admin.dto.DashboardOverviewResponse;
import com.tailoris.api.admin.dto.DashboardStatsResponse;
import com.tailoris.api.admin.dto.DashboardTrendResponse;
import com.tailoris.common.constant.RoutePortConstant;
import com.tailoris.admin.service.AdminDashboardService;
import com.tailoris.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 平台管理员仪表盘控制器。
 *
 * <p>提供核心统计、趋势、概览三类接口，供前端仪表盘多维度数据展示。
 * 全部接口受 {@link SaCheckRole} 平台管理员角色保护。</p>
 *
 * @author Tailor IS Team
 */
@SaCheckRole({"SUPER_ADMIN", "PLATFORM_ADMIN"})
@Tag(name = "平台管理员仪表盘")
@RestController
@RequestMapping(RoutePortConstant.ROUTE_ADMIN_DASHBOARD)
@RequiredArgsConstructor
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    /**
     * 获取仪表盘核心统计数据（含昨日对比与增长率）。
     *
     * @return 统计数据响应
     */
    @Operation(summary = "获取仪表盘统计数据")
    @GetMapping("/stats")
    public Result<DashboardStatsResponse> getDashboardStats() {
        DashboardStatsResponse stats = adminDashboardService.getDashboardStats();
        return Result.success(stats);
    }

    /**
     * 获取近 N 天营收与订单趋势数据。
     *
     * @param days 天数（1~90，默认 7，超出区间自动校正）
     * @return 趋势数据响应
     */
    @Operation(summary = "获取仪表盘趋势数据")
    @GetMapping("/trend")
    public Result<DashboardTrendResponse> getDashboardTrend(
            @Parameter(description = "天数，1~90，默认7") @RequestParam(defaultValue = "7") int days) {
        DashboardTrendResponse trend = adminDashboardService.getDashboardTrend(days);
        return Result.success(trend);
    }

    /**
     * 获取仪表盘概览数据（订单状态分布 + 商家/商品排行）。
     *
     * @return 概览数据响应
     */
    @Operation(summary = "获取仪表盘概览数据")
    @GetMapping("/overview")
    public Result<DashboardOverviewResponse> getDashboardOverview() {
        DashboardOverviewResponse overview = adminDashboardService.getDashboardOverview();
        return Result.success(overview);
    }
}
