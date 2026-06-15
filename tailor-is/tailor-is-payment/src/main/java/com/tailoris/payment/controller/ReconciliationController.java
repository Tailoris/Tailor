package com.tailoris.payment.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.stp.StpUtil;
import com.tailoris.common.exception.BusinessException;
import com.tailoris.common.result.Result;
import com.tailoris.payment.entity.ReconciliationRecord;
import com.tailoris.payment.service.ReconciliationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@Tag(name = "资金对账管理", description = "支付资金对账、差异处理、对账记录查询等接口")
@RestController
@RequestMapping("/api/v1/payment/reconciliation")
@RequiredArgsConstructor
public class ReconciliationController {

    private final ReconciliationService reconciliationService;

    @Operation(summary = "创建对账记录", description = "为订单创建支付对账记录")
    @PostMapping("/record")
    public Result<Void> recordPayment(@RequestParam @NotNull Long orderId,
                                      @RequestParam @NotNull BigDecimal amount,
                                      @RequestParam @NotNull String channel,
                                      @RequestParam @NotNull String channelTradeNo) {
        if (!StpUtil.isLogin()) {
            throw new BusinessException("用户未登录");
        }
        reconciliationService.recordPayment(orderId, amount, channel, channelTradeNo);
        return Result.success();
    }

    @Operation(summary = "执行单笔对账", description = "对指定订单进行对账比对，检查支付金额一致性")
    @PostMapping("/execute")
    public Result<Void> reconcile(@RequestParam @NotNull Long orderId) {
        if (!StpUtil.isLogin()) {
            throw new BusinessException("用户未登录");
        }
        reconciliationService.reconcile(orderId);
        return Result.success();
    }

    @Operation(summary = "批量对账", description = "批量执行多笔订单对账，仅限平台管理员")
    @SaCheckRole("admin")
    @PostMapping("/batch-execute")
    public Result<Void> batchReconcile(@RequestBody @NotNull List<Long> orderIds) {
        reconciliationService.batchReconcile(orderIds);
        return Result.success();
    }

    @Operation(summary = "查询未对账记录", description = "查询所有待对账的支付记录列表")
    @GetMapping("/unreconciled")
    public Result<List<ReconciliationRecord>> listUnreconciled() {
        if (!StpUtil.isLogin()) {
            throw new BusinessException("用户未登录");
        }
        List<ReconciliationRecord> records = reconciliationService.listUnreconciled();
        return Result.success(records);
    }
}