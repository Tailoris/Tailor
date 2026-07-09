package com.tailoris.admin.service.impl;

import com.tailoris.admin.dto.ReportGenerateRequest;
import com.tailoris.admin.dto.ReportQueryRequest;
import com.tailoris.admin.dto.ReportStatusUpdateRequest;
import com.tailoris.admin.dto.StatisticsResponse;
import com.tailoris.admin.feign.AnalyticsFeignClient;
import com.tailoris.admin.service.AdminAnalyticsService;
import com.tailoris.common.dto.PageResponse;
import com.tailoris.common.exception.BusinessException;
import com.tailoris.common.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminAnalyticsServiceImpl implements AdminAnalyticsService {

    private final AnalyticsFeignClient analyticsFeignClient;

    @Override
    public PageResponse<Object> listReports(ReportQueryRequest request) {
        Result<PageResponse<Object>> response = analyticsFeignClient.getReportList(request);
        if (!response.isSuccess()) {
            throw new BusinessException(response.getMessage());
        }
        return response.getData();
    }

    @Override
    public Object getReport(Long id) {
        Result<Object> response = analyticsFeignClient.getReportById(id);
        if (!response.isSuccess()) {
            throw new BusinessException(response.getMessage());
        }
        return response.getData();
    }

    @Override
    public Long generateReport(ReportGenerateRequest request) {
        Result<Long> response = analyticsFeignClient.generateReport(request);
        if (!response.isSuccess()) {
            throw new BusinessException(response.getMessage());
        }
        return response.getData();
    }

    @Override
    public void updateReportStatus(Long id, ReportStatusUpdateRequest request) {
        Result<Void> response = analyticsFeignClient.updateReportStatus(id, request);
        if (!response.isSuccess()) {
            throw new BusinessException(response.getMessage());
        }
    }

    @Override
    public StatisticsResponse getStatistics() {
        Result<StatisticsResponse> response = analyticsFeignClient.getStatistics();
        if (!response.isSuccess()) {
            throw new BusinessException(response.getMessage());
        }
        return response.getData();
    }
}