package com.tailoris.payment.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.tailoris.common.exception.BusinessException;
import com.tailoris.common.result.Result;
import com.tailoris.payment.dto.AccountInfoResponse;
import com.tailoris.payment.dto.WithdrawRequest;
import com.tailoris.payment.entity.RechargeRecord;
import com.tailoris.payment.entity.WithdrawRecord;
import com.tailoris.payment.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@SaCheckLogin
@Tag(name = "账户管理", description = "账户查询、提现、充值等接口")
@RestController
@RequestMapping("/api/account")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @Operation(summary = "获取用户账户信息", description = "获取当前登录用户的账户余额、积分等信息")
    @GetMapping("/user/info")
    public Result<AccountInfoResponse> getUserAccount() {
        if (!StpUtil.isLogin()) {
            throw new BusinessException("用户未登录");
        }
        Long userId = StpUtil.getLoginIdAsLong();
        AccountInfoResponse response = accountService.getUserAccount(userId);
        return Result.success(response);
    }

    @Operation(summary = "获取商家账户信息", description = "获取当前登录商家的账户余额、可提现金额等信息")
    @GetMapping("/merchant/info")
    public Result<AccountInfoResponse> getMerchantAccount() {
        if (!StpUtil.isLogin()) {
            throw new BusinessException("用户未登录");
        }
        Long merchantId = StpUtil.getLoginIdAsLong();
        AccountInfoResponse response = accountService.getMerchantAccount(merchantId);
        return Result.success(response);
    }

    @Operation(summary = "申请提现", description = "商家申请提现到银行账户")
    @PostMapping("/merchant/withdraw")
    public Result<Void> withdraw(@Valid @RequestBody WithdrawRequest request) {
        if (!StpUtil.isLogin()) {
            throw new BusinessException("用户未登录");
        }
        Long merchantId = StpUtil.getLoginIdAsLong();
        accountService.createWithdraw(merchantId, request);
        return Result.success();
    }

    @Operation(summary = "充值", description = "用户账户充值")
    @PostMapping("/user/recharge")
    public Result<RechargeRecord> recharge(
            @RequestParam BigDecimal amount,
            @RequestParam Integer payChannel) {
        if (!StpUtil.isLogin()) {
            throw new BusinessException("用户未登录");
        }
        Long userId = StpUtil.getLoginIdAsLong();
        RechargeRecord record = accountService.recharge(userId, amount, payChannel);
        return Result.success(record);
    }

    @Operation(summary = "充值回调", description = "充值支付回调")
    @PostMapping("/user/recharge/callback")
    public Result<Void> rechargeCallback(
            @RequestParam String rechargeNo,
            @RequestParam String transactionId) {
        accountService.payRechargeCallback(rechargeNo, transactionId);
        return Result.success();
    }

    @Operation(summary = "查询提现记录", description = "根据提现单号查询提现记录")
    @GetMapping("/merchant/withdraw/detail")
    public Result<WithdrawRecord> getWithdrawDetail(@RequestParam String withdrawNo) {
        WithdrawRecord record = accountService.getWithdrawByNo(withdrawNo);
        return Result.success(record);
    }
}
