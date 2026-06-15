package com.tailoris.payment.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.tailoris.common.exception.BusinessException;
import com.tailoris.common.result.Result;
import com.tailoris.payment.dto.PayRequest;
import com.tailoris.payment.dto.RefundRequest;
import com.tailoris.payment.entity.PaymentRecord;
import com.tailoris.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Deprecated
@SaCheckLogin
@Tag(name = "支付管理", description = "已弃用，请使用 /api/v1/payment")
@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @Operation(summary = "创建支付", description = "为订单创建支付记录")
    @PostMapping("/create")
    public Result<PaymentRecord> createPayment(@Valid @RequestBody PayRequest request) {
        if (!StpUtil.isLogin()) {
            throw new BusinessException("用户未登录");
        }
        Long userId = StpUtil.getLoginIdAsLong();
        PaymentRecord record = paymentService.createPayment(userId, request);
        return Result.success(record);
    }

    @Operation(summary = "支付回调", description = "支付渠道异步回调通知")
    @PostMapping("/callback")
    public Result<Void> payCallback(
            @RequestParam String paymentNo,
            @RequestParam String transactionId,
            @RequestParam(required = false) String channelResponse,
            @RequestParam Long merchantId) {
        paymentService.payCallback(paymentNo, transactionId, channelResponse, merchantId);
        return Result.success();
    }

    @Operation(summary = "申请退款", description = "为售后工单申请退款")
    @PostMapping("/refund")
    public Result<Void> refund(@Valid @RequestBody RefundRequest request) {
        if (!StpUtil.isLogin()) {
            throw new BusinessException("用户未登录");
        }
        Long userId = StpUtil.getLoginIdAsLong();
        paymentService.refund(userId, request.getTicketId(), null, request.getAmount(), request.getRefundChannel(), request.getRemark());
        return Result.success();
    }

    @Operation(summary = "查询支付状态", description = "根据支付ID查询支付状态")
    @GetMapping("/status")
    public Result<PaymentRecord> getPaymentStatus(@RequestParam Long paymentId) {
        PaymentRecord record = paymentService.getPaymentStatus(paymentId);
        return Result.success(record);
    }
}
