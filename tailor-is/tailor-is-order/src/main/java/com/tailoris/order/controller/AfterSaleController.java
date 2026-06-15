package com.tailoris.order.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tailoris.common.result.Result;
import com.tailoris.order.dto.AfterSaleRequest;
import com.tailoris.order.dto.TicketProcessRequest;
import com.tailoris.order.entity.AfterSaleTicket;
import com.tailoris.order.service.AfterSaleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@SaCheckLogin
@Tag(name = "售后管理")
@RestController
@RequestMapping("/api/order/after-sale")
@RequiredArgsConstructor
public class AfterSaleController {

    private final AfterSaleService afterSaleService;

    @Operation(summary = "创建售后工单")
    @PostMapping
    public Result<String> createTicket(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody AfterSaleRequest request) {
        String ticketNo = afterSaleService.createTicket(userId, request);
        return Result.success(ticketNo);
    }

    @Operation(summary = "获取售后工单详情")
    @GetMapping("/{ticketNo}")
    public Result<AfterSaleTicket> getTicketDetail(
            @PathVariable String ticketNo) {
        AfterSaleTicket ticket = afterSaleService.getTicketDetail(ticketNo);
        return Result.success(ticket);
    }

    @Operation(summary = "查询用户售后工单列表")
    @GetMapping("/list")
    public Result<Page<AfterSaleTicket>> listTickets(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        Page<AfterSaleTicket> page = afterSaleService.listTicketsByUser(userId, status, pageNum, pageSize);
        return Result.success(page);
    }

    @Operation(summary = "处理售后工单")
    @PutMapping("/{ticketNo}/process")
    public Result<Void> processTicket(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody TicketProcessRequest request) {
        afterSaleService.processTicket(userId, request);
        return Result.success();
    }
}
