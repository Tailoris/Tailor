package com.tailoris.order.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.tailoris.common.result.Result;
import com.tailoris.order.dto.LogisticsUpdateRequest;
import com.tailoris.order.entity.OrderLogistics;
import com.tailoris.order.service.OrderLogisticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@SaCheckLogin
@Tag(name = "物流管理")
@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
public class LogisticsController {

    private final OrderLogisticsService orderLogisticsService;

    @Operation(summary = "更新物流信息")
    @PutMapping("/{orderId}/logistics")
    public Result<Void> updateLogistics(
            @PathVariable Long orderId,
            @Valid @RequestBody LogisticsUpdateRequest request) {
        orderLogisticsService.updateLogistics(orderId, request.getLogisticsCompany(), request.getLogisticsCompanyName(), request.getLogisticsNo());
        return Result.success();
    }

    @Operation(summary = "获取物流信息")
    @GetMapping("/{orderId}/logistics")
    public Result<OrderLogistics> getLogistics(
            @PathVariable Long orderId) {
        OrderLogistics logistics = orderLogisticsService.getLogisticsByOrderId(orderId);
        return Result.success(logistics);
    }

    @Operation(summary = "物流轨迹查询")
    @GetMapping("/logistics/track")
    public Result<Object> trackLogistics(
            @RequestParam String logisticsNo) {
        Object trackInfo = orderLogisticsService.trackLogistics(logisticsNo);
        return Result.success(trackInfo);
    }
}
