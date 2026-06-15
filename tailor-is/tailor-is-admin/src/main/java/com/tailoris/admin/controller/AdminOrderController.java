package com.tailoris.admin.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.tailoris.api.admin.dto.ArbitrateRequest;
import com.tailoris.api.admin.dto.OrderQueryRequest;
import com.tailoris.admin.service.AdminOrderService;
import com.tailoris.common.dto.PageResponse;
import com.tailoris.common.result.Result;
import com.tailoris.api.order.entity.OrderInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@SaCheckRole("admin")
@Tag(name = "平台订单管理")
@RestController
@RequestMapping("/api/admin/order")
@RequiredArgsConstructor
public class AdminOrderController {

    private final AdminOrderService adminOrderService;

    @Operation(summary = "查询所有订单列表")
    @GetMapping("/list")
    public Result<PageResponse<OrderInfo>> listOrders(OrderQueryRequest request) {
        PageResponse<OrderInfo> page = adminOrderService.listAllOrders(request);
        return Result.success(page);
    }

    @Operation(summary = "获取订单详情")
    @GetMapping("/{orderNo}")
    public Result<OrderInfo> getOrderDetail(@PathVariable String orderNo) {
        OrderInfo orderInfo = adminOrderService.getOrderDetail(orderNo);
        return Result.success(orderInfo);
    }

    @Operation(summary = "仲裁售后争议")
    @PutMapping("/arbitrate")
    public Result<Void> arbitrate(@Valid @RequestBody ArbitrateRequest request) {
        adminOrderService.arbitrateDispute(request, null);
        return Result.success();
    }
}
