package com.tailoris.admin.service.impl;

import com.tailoris.admin.dto.PatternAuditRequest;
import com.tailoris.admin.dto.PatternQueryRequest;
import com.tailoris.admin.dto.PatternScoreRequest;
import com.tailoris.admin.feign.PatternFeignClient;
import com.tailoris.admin.service.AdminPatternService;
import com.tailoris.common.dto.PageResponse;
import com.tailoris.common.exception.BusinessException;
import com.tailoris.common.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminPatternServiceImpl implements AdminPatternService {

    private final PatternFeignClient patternFeignClient;

    @Override
    public PageResponse<Object> listPatterns(PatternQueryRequest request) {
        Result<PageResponse<Object>> response = patternFeignClient.getPatternList(request);
        if (!response.isSuccess()) {
            throw new BusinessException(response.getMessage());
        }
        return response.getData();
    }

    @Override
    public Object getPattern(Long id) {
        Result<Object> response = patternFeignClient.getPatternById(id);
        if (!response.isSuccess()) {
            throw new BusinessException(response.getMessage());
        }
        return response.getData();
    }

    @Override
    public void auditPattern(Long id, PatternAuditRequest request) {
        Result<Void> response = patternFeignClient.auditPattern(id, request);
        if (!response.isSuccess()) {
            throw new BusinessException(response.getMessage());
        }
    }

    @Override
    public void updatePatternScore(Long id, PatternScoreRequest request) {
        Result<Void> response = patternFeignClient.updatePatternScore(id, request);
        if (!response.isSuccess()) {
            throw new BusinessException(response.getMessage());
        }
    }

    @Override
    public void deletePattern(Long id) {
        Result<Void> response = patternFeignClient.deletePattern(id);
        if (!response.isSuccess()) {
            throw new BusinessException(response.getMessage());
        }
    }
}