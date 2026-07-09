package com.tailoris.admin.feign;

import com.tailoris.admin.dto.FabricQueryRequest;
import com.tailoris.admin.dto.SupplierAuditRequest;
import com.tailoris.admin.dto.SupplierQueryRequest;
import com.tailoris.common.dto.PageResponse;
import com.tailoris.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "tailor-is-supply")
public interface SupplyFeignClient {

    @GetMapping("/api/v1/supply/fabrics")
    Result<PageResponse<Object>> getFabricList(@RequestBody FabricQueryRequest request);

    @GetMapping("/api/v1/supply/fabrics/{id}")
    Result<Object> getFabricById(@PathVariable Long id);

    @GetMapping("/api/v1/supply/suppliers")
    Result<PageResponse<Object>> getSupplierList(@RequestBody SupplierQueryRequest request);

    @PutMapping("/api/v1/supply/suppliers/{id}/audit")
    Result<Void> auditSupplier(@PathVariable Long id, @RequestBody SupplierAuditRequest request);

    @GetMapping("/api/v1/supply/purchase-orders")
    Result<PageResponse<Object>> getPurchaseOrderList();
}