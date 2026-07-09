package com.tailoris.admin.service;

import com.tailoris.admin.dto.ReportGenerateRequest;
import com.tailoris.admin.dto.ReportQueryRequest;
import com.tailoris.admin.dto.ReportStatusUpdateRequest;
import com.tailoris.admin.dto.StatisticsResponse;
import com.tailoris.common.dto.PageResponse;

public interface AdminAnalyticsService {

    PageResponse<Object> listReports(ReportQueryRequest request);

    Object getReport(Long id);

    Long generateReport(ReportGenerateRequest request);

    void updateReportStatus(Long id, ReportStatusUpdateRequest request);

    StatisticsResponse getStatistics();
}