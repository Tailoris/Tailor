package com.tailoris.admin.feign;

import com.tailoris.admin.dto.ReportGenerateRequest;
import com.tailoris.admin.dto.ReportQueryRequest;
import com.tailoris.admin.dto.ReportStatusUpdateRequest;
import com.tailoris.admin.dto.StatisticsResponse;
import com.tailoris.common.dto.PageResponse;
import com.tailoris.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "tailor-is-analytics")
public interface AnalyticsFeignClient {

    @GetMapping("/api/v1/analytics/reports")
    Result<PageResponse<Object>> getReportList(@RequestBody ReportQueryRequest request);

    @GetMapping("/api/v1/analytics/reports/{id}")
    Result<Object> getReportById(@PathVariable Long id);

    @PostMapping("/api/v1/analytics/reports")
    Result<Long> generateReport(@RequestBody ReportGenerateRequest request);

    @PutMapping("/api/v1/analytics/reports/{id}/status")
    Result<Void> updateReportStatus(@PathVariable Long id, @RequestBody ReportStatusUpdateRequest request);

    @GetMapping("/api/v1/analytics/statistics")
    Result<StatisticsResponse> getStatistics();
}