package com.tailoris.admin.service.impl;

import com.tailoris.admin.dto.FabricQueryRequest;
import com.tailoris.admin.dto.SupplierAuditRequest;
import com.tailoris.admin.dto.SupplierQueryRequest;
import com.tailoris.admin.feign.SupplyFeignClient;
import com.tailoris.admin.service.AdminSupplyService;
import com.tailoris.common.dto.PageResponse;
import com.tailoris.common.exception.BusinessException;
import com.tailoris.common.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminSupplyServiceImpl implements AdminSupplyService {

    private final SupplyFeignClient supplyFeignClient;

    @Override
    public PageResponse<Object> listFabrics(FabricQueryRequest request) {
        Result<PageResponse<Object>> response = supplyFeignClient.getFabricList(request);
        if (!response.isSuccess()) {
            throw new BusinessException(response.getMessage());
        }
        return response.getData();
    }

    @Override
    public Object getFabric(Long id) {
        Result<Object> response = supplyFeignClient.getFabricById(id);
        if (!response.isSuccess()) {
            throw new BusinessException(response.getMessage());
        }
        return response.getData();
    }

    @Override
    public PageResponse<Object> listSuppliers(SupplierQueryRequest request) {
        Result<PageResponse<Object>> response = supplyFeignClient.getSupplierList(request);
        if (!response.isSuccess()) {
            throw new BusinessException(response.getMessage());
        }
        return response.getData();
    }

    @Override
    public void auditSupplier(Long id, SupplierAuditRequest request) {
        Result<Void> response = supplyFeignClient.auditSupplier(id, request);
        if (!response.isSuccess()) {
            throw new BusinessException(response.getMessage());
        }
    }

    @Override
    public PageResponse<Object> listPurchaseOrders(Integer status, Integer pageNum, Integer pageSize) {
        Result<PageResponse<Object>> response = supplyFeignClient.getPurchaseOrderList();
        if (!response.isSuccess()) {
            throw new BusinessException(response.getMessage());
        }
        return response.getData();
    }
}