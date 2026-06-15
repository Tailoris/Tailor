package com.tailoris.admin.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.tailoris.api.admin.dto.MerchantAuditRequest;
import com.tailoris.api.admin.dto.MerchantQueryRequest;
import com.tailoris.admin.service.AdminMerchantService;
import com.tailoris.common.dto.PageResponse;
import com.tailoris.common.result.Result;
import com.tailoris.api.merchant.entity.Merchant;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@SaCheckRole("admin")
@Tag(name = "平台商家管理")
@RestController
@RequestMapping("/api/admin/merchant")
@RequiredArgsConstructor
public class AdminMerchantController {

    private final AdminMerchantService adminMerchantService;

    @Operation(summary = "查询待审核商家列表")
    @GetMapping("/pending")
    public Result<PageResponse<Merchant>> listPending(MerchantQueryRequest request) {
        PageResponse<Merchant> page = adminMerchantService.listPendingMerchants(request);
        return Result.success(page);
    }

    @Operation(summary = "商家审核")
    @PutMapping("/audit/{id}")
    public Result<Void> audit(@PathVariable Long id, @Valid @RequestBody MerchantAuditRequest request) {
        request.setMerchantId(id);
        adminMerchantService.auditMerchant(request, null);
        return Result.success();
    }

    @Operation(summary = "冻结商家")
    @PutMapping("/freeze/{id}")
    public Result<Void> freeze(@PathVariable Long id) {
        adminMerchantService.freezeMerchant(id);
        return Result.success();
    }

    @Operation(summary = "解冻商家")
    @PutMapping("/unfreeze/{id}")
    public Result<Void> unfreeze(@PathVariable Long id) {
        adminMerchantService.unfreezeMerchant(id);
        return Result.success();
    }

    @Operation(summary = "查询商家列表")
    @GetMapping("/list")
    public Result<PageResponse<Merchant>> listMerchants(MerchantQueryRequest request) {
        PageResponse<Merchant> page = adminMerchantService.listMerchants(request);
        return Result.success(page);
    }

    @Operation(summary = "获取商家详情")
    @GetMapping("/{id}")
    public Result<Merchant> getMerchantDetail(@PathVariable Long id) {
        Merchant merchant = adminMerchantService.getMerchantDetail(id);
        return Result.success(merchant);
    }
}
