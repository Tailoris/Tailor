package com.tailoris.merchant.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.tailoris.common.dto.PageResponse;
import com.tailoris.common.result.Result;
import com.tailoris.merchant.dto.MerchantApplyRequest;
import com.tailoris.merchant.dto.MerchantAuditRequest;
import com.tailoris.merchant.dto.MerchantQueryRequest;
import com.tailoris.merchant.entity.Merchant;
import com.tailoris.merchant.service.MerchantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@SaCheckLogin
@Tag(name = "商家管理")
@RestController
@RequestMapping("/api/merchant")
@RequiredArgsConstructor
public class MerchantController {

    private final MerchantService merchantService;

    @Operation(summary = "商家入驻申请")
    @PostMapping("/apply")
    public Result<Void> applyJoin(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @Valid @RequestBody MerchantApplyRequest request) {
        if (userId == null) {
            return Result.fail("用户未登录");
        }
        merchantService.applyJoin(userId, request);
        return Result.success();
    }

    @Operation(summary = "获取商家信息")
    @GetMapping("/info")
    public Result<Merchant> getMerchantInfo(
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        if (userId == null) {
            return Result.fail("用户未登录");
        }
        Merchant merchant = merchantService.getMerchantInfo(userId);
        if (merchant == null) {
            return Result.fail("商家信息不存在");
        }
        return Result.success(merchant);
    }

    @Operation(summary = "获取商家信息（通过商家ID）")
    @GetMapping("/info/{id}")
    public Result<Merchant> getMerchantById(@PathVariable Long id) {
        Merchant merchant = merchantService.getMerchantById(id);
        if (merchant == null) {
            return Result.fail("商家信息不存在");
        }
        return Result.success(merchant);
    }

    @Operation(summary = "查询商家列表")
    @GetMapping("/list")
    public Result<PageResponse<Merchant>> listMerchants(MerchantQueryRequest request) {
        PageResponse<Merchant> page = merchantService.listMerchants(request);
        return Result.success(page);
    }

    @Operation(summary = "商家审核")
    @PutMapping("/audit")
    public Result<Void> audit(@Valid @RequestBody MerchantAuditRequest request) {
        merchantService.audit(request);
        return Result.success();
    }
}
