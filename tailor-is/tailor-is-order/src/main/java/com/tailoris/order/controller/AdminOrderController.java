package com.tailoris.order.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tailoris.common.result.Result;
import com.tailoris.order.entity.AfterSaleTicket;
import com.tailoris.order.entity.OrderInfo;
import com.tailoris.order.mapper.AfterSaleTicketMapper;
import com.tailoris.order.mapper.OrderInfoMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@SaCheckRole("admin")
@Tag(name = "后台订单管理")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminOrderController {

    private final OrderInfoMapper orderInfoMapper;
    private final AfterSaleTicketMapper afterSaleTicketMapper;

    @Operation(summary = "后台查询订单列表")
    @GetMapping("/order/list")
    public Result<Page<OrderInfo>> listOrders(
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Integer productType,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long merchantId,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        LambdaQueryWrapper<OrderInfo> queryWrapper = new LambdaQueryWrapper<>();

        if (status != null) {
            queryWrapper.eq(OrderInfo::getStatus, status);
        }
        if (productType != null) {
            queryWrapper.eq(OrderInfo::getProductType, productType);
        }
        if (keyword != null && !keyword.isEmpty()) {
            queryWrapper.like(OrderInfo::getOrderNo, keyword);
        }
        if (merchantId != null) {
            queryWrapper.eq(OrderInfo::getMerchantId, merchantId);
        }

        queryWrapper.orderByDesc(OrderInfo::getCreateTime);

        Page<OrderInfo> page = new Page<>(pageNum, pageSize);
        Page<OrderInfo> result = orderInfoMapper.selectPage(page, queryWrapper);
        return Result.success(result);
    }

    @Operation(summary = "订单统计")
    @GetMapping("/order/stats")
    public Result<Map<String, Object>> getOrderStats() {
        Map<String, Object> stats = new HashMap<>();

        LambdaQueryWrapper<OrderInfo> totalQuery = new LambdaQueryWrapper<>();
        Long totalOrders = orderInfoMapper.selectCount(totalQuery);
        stats.put("totalOrders", totalOrders);

        LambdaQueryWrapper<OrderInfo> todayQuery = new LambdaQueryWrapper<>();
        todayQuery.apply("DATE(created_at) = CURDATE()");
        Long todayOrders = orderInfoMapper.selectCount(todayQuery);
        stats.put("todayOrders", todayOrders);

        LambdaQueryWrapper<OrderInfo> pendingPayQuery = new LambdaQueryWrapper<>();
        pendingPayQuery.eq(OrderInfo::getStatus, 0);
        Long pendingPayOrders = orderInfoMapper.selectCount(pendingPayQuery);
        stats.put("pendingPayOrders", pendingPayOrders);

        LambdaQueryWrapper<OrderInfo> pendingDeliveryQuery = new LambdaQueryWrapper<>();
        pendingDeliveryQuery.eq(OrderInfo::getStatus, 1);
        Long pendingDeliveryOrders = orderInfoMapper.selectCount(pendingDeliveryQuery);
        stats.put("pendingDeliveryOrders", pendingDeliveryOrders);

        LambdaQueryWrapper<OrderInfo> completedQuery = new LambdaQueryWrapper<>();
        completedQuery.eq(OrderInfo::getStatus, 3);
        Long completedOrders = orderInfoMapper.selectCount(completedQuery);
        stats.put("completedOrders", completedOrders);

        stats.put("totalAmount", BigDecimal.ZERO);

        return Result.success(stats);
    }

    @Operation(summary = "待处理售后工单")
    @GetMapping("/after-sale/pending")
    public Result<Page<AfterSaleTicket>> listPendingTickets(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        LambdaQueryWrapper<AfterSaleTicket> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AfterSaleTicket::getStatus, 0)
                .orderByAsc(AfterSaleTicket::getCreateTime);

        Page<AfterSaleTicket> page = new Page<>(pageNum, pageSize);
        Page<AfterSaleTicket> result = afterSaleTicketMapper.selectPage(page, queryWrapper);
        return Result.success(result);
    }

    @Operation(summary = "平台仲裁售后工单")
    @PutMapping("/after-sale/arbitrate")
    public Result<Void> arbitrateTicket(
            @RequestParam Long ticketId,
            @RequestParam Integer result,
            @RequestParam(required = false) String remark) {
        AfterSaleTicket ticket = afterSaleTicketMapper.selectById(ticketId);
        if (ticket == null) {
            return Result.fail("售后工单不存在");
        }

        if (result == 1) {
            ticket.setStatus(2);
        } else {
            ticket.setStatus(3);
        }
        ticket.setPlatformIntervene(1);
        ticket.setPlatformResult(remark);
        ticket.setProcessedAt(java.time.LocalDateTime.now());
        afterSaleTicketMapper.updateById(ticket);

        return Result.success();
    }
}
