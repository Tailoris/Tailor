package com.tailoris.admin.service;

import com.tailoris.admin.dto.FabricQueryRequest;
import com.tailoris.admin.dto.SupplierAuditRequest;
import com.tailoris.admin.dto.SupplierQueryRequest;
import com.tailoris.common.dto.PageResponse;

public interface AdminSupplyService {

    PageResponse<Object> listFabrics(FabricQueryRequest request);

    Object getFabric(Long id);

    PageResponse<Object> listSuppliers(SupplierQueryRequest request);

    void auditSupplier(Long id, SupplierAuditRequest request);

    PageResponse<Object> listPurchaseOrders(Integer status, Integer pageNum, Integer pageSize);
}