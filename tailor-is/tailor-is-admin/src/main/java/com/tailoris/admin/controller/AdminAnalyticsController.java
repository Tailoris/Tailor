package com.tailoris.admin.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.tailoris.admin.dto.ReportGenerateRequest;
import com.tailoris.common.constant.RoutePortConstant;
import com.tailoris.admin.dto.ReportQueryRequest;
import com.tailoris.admin.dto.ReportStatusUpdateRequest;
import com.tailoris.admin.dto.StatisticsResponse;
import com.tailoris.admin.service.AdminAnalyticsService;
import com.tailoris.common.dto.PageResponse;
import com.tailoris.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@SaCheckRole({"SUPER_ADMIN", "PLATFORM_ADMIN", "DATA_ADMIN"})
@Tag(name = "数据分析管理")
@RestController
@RequestMapping(RoutePortConstant.ROUTE_ADMIN_ANALYTICS)
@RequiredArgsConstructor
public class AdminAnalyticsController {

    private final AdminAnalyticsService adminAnalyticsService;

    @Operation(summary = "查询报告列表")
    @GetMapping("/reports")
    public Result<PageResponse<Object>> listReports(ReportQueryRequest request) {
        PageResponse<Object> page = adminAnalyticsService.listReports(request);
        return Result.success(page);
    }

    @Operation(summary = "获取报告详情")
    @GetMapping("/reports/{id}")
    public Result<Object> getReport(@Parameter(description = "报告ID") @PathVariable Long id) {
        Object report = adminAnalyticsService.getReport(id);
        return Result.success(report);
    }

    @Operation(summary = "生成分析报告")
    @PostMapping("/reports")
    public Result<Long> generateReport(@Valid @RequestBody ReportGenerateRequest request) {
        Long reportId = adminAnalyticsService.generateReport(request);
        return Result.success(reportId);
    }

    @Operation(summary = "更新报告状态")
    @PutMapping("/reports/{id}/status")
    public Result<Void> updateReportStatus(
            @Parameter(description = "报告ID") @PathVariable Long id,
            @Valid @RequestBody ReportStatusUpdateRequest request) {
        adminAnalyticsService.updateReportStatus(id, request);
        return Result.success();
    }

    @Operation(summary = "获取数据统计")
    @GetMapping("/statistics")
    public Result<StatisticsResponse> getStatistics() {
        StatisticsResponse statistics = adminAnalyticsService.getStatistics();
        return Result.success(statistics);
    }
}