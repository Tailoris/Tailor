package com.tailoris.analytics.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.tailoris.analytics.entity.MetricsSnapshot;
import com.tailoris.analytics.service.AnalyticsService;
import com.tailoris.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@SaCheckLogin
@Slf4j
@Tag(name = "数据分析")
@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
public class MetricsController {

    private final AnalyticsService analyticsService;

    @Operation(summary = "查询指标")
    @GetMapping("/metrics/{type}")
    public Result<List<MetricsSnapshot>> getMetrics(
            @PathVariable String type,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {
        List<MetricsSnapshot> metrics = analyticsService.getMetricsByTypeAndDateRange(
                type,
                startDate != null ? startDate : LocalDate.now().minusDays(30),
                endDate != null ? endDate : LocalDate.now()
        );
        return Result.success(metrics);
    }

    @Operation(summary = "记录指标")
    @PostMapping("/metrics")
    public Result<Void> recordMetric(
            @RequestParam String metricType,
            @RequestParam String metricKey,
            @RequestParam BigDecimal metricValue,
            @RequestParam(required = false) String snapshotDate,
            @RequestParam(required = false, defaultValue = "") String dimension) {
        LocalDate date = snapshotDate != null ? LocalDate.parse(snapshotDate) : LocalDate.now();
        analyticsService.recordMetric(metricType, metricKey, metricValue, date, dimension);
        return Result.success();
    }
}