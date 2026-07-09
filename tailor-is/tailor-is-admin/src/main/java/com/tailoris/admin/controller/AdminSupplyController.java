package com.tailoris.admin.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.tailoris.admin.dto.FabricQueryRequest;
import com.tailoris.common.constant.RoutePortConstant;
import com.tailoris.admin.dto.SupplierAuditRequest;
import com.tailoris.admin.dto.SupplierQueryRequest;
import com.tailoris.admin.service.AdminSupplyService;
import com.tailoris.common.dto.PageResponse;
import com.tailoris.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@SaCheckRole({"SUPER_ADMIN", "PLATFORM_ADMIN", "SUPPLY_ADMIN"})
@Tag(name = "供应链管理")
@RestController
@RequestMapping(RoutePortConstant.ROUTE_ADMIN_SUPPLY)
@RequiredArgsConstructor
public class AdminSupplyController {

    private final AdminSupplyService adminSupplyService;

    @Operation(summary = "查询面料列表")
    @GetMapping("/fabrics")
    public Result<PageResponse<Object>> listFabrics(FabricQueryRequest request) {
        PageResponse<Object> page = adminSupplyService.listFabrics(request);
        return Result.success(page);
    }

    @Operation(summary = "获取面料详情")
    @GetMapping("/fabrics/{id}")
    public Result<Object> getFabric(@Parameter(description = "面料ID") @PathVariable Long id) {
        Object fabric = adminSupplyService.getFabric(id);
        return Result.success(fabric);
    }

    @Operation(summary = "查询供应商列表")
    @GetMapping("/suppliers")
    public Result<PageResponse<Object>> listSuppliers(SupplierQueryRequest request) {
        PageResponse<Object> page = adminSupplyService.listSuppliers(request);
        return Result.success(page);
    }

    @Operation(summary = "审核供应商")
    @PutMapping("/suppliers/{id}/audit")
    public Result<Void> auditSupplier(
            @Parameter(description = "供应商ID") @PathVariable Long id,
            @RequestBody SupplierAuditRequest request) {
        adminSupplyService.auditSupplier(id, request);
        return Result.success();
    }

    @Operation(summary = "查询采购订单")
    @GetMapping("/purchase-orders")
    public Result<PageResponse<Object>> listPurchaseOrders(
            @Parameter(description = "订单状态") @RequestParam(required = false) Integer status,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") Integer pageSize) {
        PageResponse<Object> page = adminSupplyService.listPurchaseOrders(status, pageNum, pageSize);
        return Result.success(page);
    }
}