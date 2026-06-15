package com.tailoris.order.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tailoris.common.result.Result;
import com.tailoris.order.dto.CreateOrderRequest;
import com.tailoris.order.dto.OrderQueryRequest;
import com.tailoris.order.entity.OrderInfo;
import com.tailoris.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@SaCheckLogin
@Deprecated
@Tag(name = "订单管理", description = "已弃用，请使用 /api/v1/order")
@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @Operation(summary = "创建订单")
    @PostMapping
    public Result<String> createOrder(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody CreateOrderRequest request) {
        String orderNo = orderService.createOrder(userId, request);
        return Result.success(orderNo);
    }

    @Operation(summary = "获取订单详情")
    @GetMapping("/{orderNo}")
    public Result<OrderInfo> getOrderDetail(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable String orderNo) {
        OrderInfo order = orderService.getOrderDetail(orderNo);
        return Result.success(order);
    }

    @Operation(summary = "查询订单列表")
    @GetMapping("/list")
    public Result<Page<OrderInfo>> listOrders(
            @RequestHeader("X-User-Id") Long userId,
            OrderQueryRequest request) {
        Page<OrderInfo> page = orderService.listOrders(userId, request);
        return Result.success(page);
    }

    @Operation(summary = "支付订单")
    @PutMapping("/{orderNo}/pay")
    public Result<Void> payOrder(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable String orderNo,
            @RequestParam Integer payType) {
        orderService.payOrder(userId, orderNo, payType);
        return Result.success();
    }

    @Operation(summary = "确认收货")
    @PutMapping("/{orderNo}/confirm")
    public Result<Void> confirmReceive(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable String orderNo) {
        orderService.confirmReceive(userId, orderNo);
        return Result.success();
    }

    @Operation(summary = "取消订单")
    @PutMapping("/{orderNo}/cancel")
    public Result<Void> cancelOrder(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable String orderNo,
            @RequestParam String reason) {
        orderService.cancelOrder(userId, orderNo, reason);
        return Result.success();
    }
}
