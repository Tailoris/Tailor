package com.tailoris.admin.service.impl;

import com.tailoris.admin.dto.CopyrightAuditRequest;
import com.tailoris.admin.dto.CopyrightQueryRequest;
import com.tailoris.admin.feign.CopyrightFeignClient;
import com.tailoris.admin.service.AdminCopyrightService;
import com.tailoris.common.dto.PageResponse;
import com.tailoris.common.exception.BusinessException;
import com.tailoris.common.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminCopyrightServiceImpl implements AdminCopyrightService {

    private final CopyrightFeignClient copyrightFeignClient;

    @Override
    public PageResponse<Object> listCopyrights(CopyrightQueryRequest request) {
        Result<PageResponse<Object>> response = copyrightFeignClient.getCopyrightList(request);
        if (!response.isSuccess()) {
            throw new BusinessException(response.getMessage());
        }
        return response.getData();
    }

    @Override
    public Object getCopyright(Long id) {
        Result<Object> response = copyrightFeignClient.getCopyrightById(id);
        if (!response.isSuccess()) {
            throw new BusinessException(response.getMessage());
        }
        return response.getData();
    }

    @Override
    public void auditCopyright(Long id, CopyrightAuditRequest request) {
        Result<Void> response = copyrightFeignClient.auditCopyright(id, request);
        if (!response.isSuccess()) {
            throw new BusinessException(response.getMessage());
        }
    }

    @Override
    public void detectInfringement(Long id) {
        Result<Void> response = copyrightFeignClient.detectInfringement(id);
        if (!response.isSuccess()) {
            throw new BusinessException(response.getMessage());
        }
    }

    @Override
    public PageResponse<Object> getEvidenceRecords(Long id) {
        Result<PageResponse<Object>> response = copyrightFeignClient.getEvidenceRecords(id);
        if (!response.isSuccess()) {
            throw new BusinessException(response.getMessage());
        }
        return response.getData();
    }
}