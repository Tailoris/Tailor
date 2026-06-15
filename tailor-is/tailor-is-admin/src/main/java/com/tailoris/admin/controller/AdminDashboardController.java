package com.tailoris.admin.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.tailoris.api.admin.dto.DashboardStatsResponse;
import com.tailoris.admin.service.AdminDashboardService;
import com.tailoris.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@SaCheckRole("admin")
@Tag(name = "平台管理员仪表盘")
@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    @Operation(summary = "获取仪表盘统计数据")
    @GetMapping("/stats")
    public Result<DashboardStatsResponse> getDashboardStats() {
        DashboardStatsResponse stats = adminDashboardService.getDashboardStats();
        return Result.success(stats);
    }
}
