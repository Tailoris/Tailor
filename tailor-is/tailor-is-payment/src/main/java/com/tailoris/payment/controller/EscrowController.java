package com.tailoris.payment.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.stp.StpUtil;
import com.tailoris.common.exception.BusinessException;
import com.tailoris.common.result.Result;
import com.tailoris.payment.entity.EscrowAccount;
import com.tailoris.payment.service.EscrowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@Tag(name = "担保账户", description = "平台担保账户余额查询、资金管理、冻结/解冻等接口")
@RestController
@RequestMapping("/api/v1/payment/escrow")
@RequiredArgsConstructor
public class EscrowController {

    private final EscrowService escrowService;

    @Operation(summary = "查询担保账户余额", description = "查询指定商户的担保账户余额")
    @GetMapping("/balance")
    public Result<BigDecimal> getBalance(@RequestParam @NotNull Long merchantId) {
        if (!StpUtil.isLogin()) {
            throw new BusinessException("用户未登录");
        }
        BigDecimal balance = escrowService.getBalance(merchantId);
        return Result.success(balance);
    }

    @Operation(summary = "查询担保账户详情", description = "查询指定商户担保账户的完整信息，包含余额和冻结金额")
    @GetMapping("/detail")
    public Result<Map<String, Object>> getDetail(@RequestParam @NotNull Long merchantId) {
        if (!StpUtil.isLogin()) {
            throw new BusinessException("用户未登录");
        }
        BigDecimal balance = escrowService.getBalance(merchantId);
        Map<String, Object> detail = Map.of(
                "merchantId", merchantId,
                "balance", balance,
                "status", "ACTIVE"
        );
        return Result.success(detail);
    }

    @Operation(summary = "平台入金", description = "平台向商户担保账户存入资金，仅限平台管理员操作")
    @SaCheckRole("admin")
    @PostMapping("/deposit")
    public Result<Void> deposit(@RequestParam @NotNull Long merchantId,
                                @RequestParam @NotNull BigDecimal amount) {
        escrowService.deposit(merchantId, amount);
        return Result.success();
    }

    @Operation(summary = "释放资金", description = "释放担保账户中未被冻结的可用资金给商户")
    @SaCheckRole("admin")
    @PostMapping("/release")
    public Result<Void> release(@RequestParam @NotNull Long merchantId,
                                @RequestParam @NotNull BigDecimal amount) {
        escrowService.release(merchantId, amount);
        return Result.success();
    }

    @Operation(summary = "冻结资金", description = "冻结担保账户部分资金，用于争议处理，仅限平台管理员操作")
    @SaCheckRole("admin")
    @PostMapping("/freeze")
    public Result<Void> freeze(@RequestParam @NotNull Long merchantId,
                               @RequestParam @NotNull BigDecimal amount) {
        escrowService.freeze(merchantId, amount);
        return Result.success();
    }

    @Operation(summary = "解冻资金", description = "解冻担保账户已冻结的资金，仅限平台管理员操作")
    @SaCheckRole("admin")
    @PostMapping("/unfreeze")
    public Result<Void> unfreeze(@RequestParam @NotNull Long merchantId,
                                 @RequestParam @NotNull BigDecimal amount) {
        escrowService.unfreeze(merchantId, amount);
        return Result.success();
    }
}