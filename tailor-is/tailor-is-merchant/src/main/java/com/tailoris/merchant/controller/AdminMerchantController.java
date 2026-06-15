package com.tailoris.merchant.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.tailoris.common.dto.PageResponse;
import com.tailoris.common.result.Result;
import com.tailoris.merchant.dto.MerchantAuditRequest;
import com.tailoris.merchant.dto.MerchantQueryRequest;
import com.tailoris.merchant.dto.QualificationAuditRequest;
import com.tailoris.merchant.entity.Merchant;
import com.tailoris.merchant.entity.MerchantQualification;
import com.tailoris.merchant.service.MerchantQualificationService;
import com.tailoris.merchant.service.MerchantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SaCheckRole("admin")
@Tag(name = "平台商家管理（管理员）")
@RestController
@RequestMapping("/api/admin/merchant")
@RequiredArgsConstructor
public class AdminMerchantController {

    private final MerchantService merchantService;
    private final MerchantQualificationService merchantQualificationService;

    @Operation(summary = "查询待审核商家列表")
    @GetMapping("/pending")
    public Result<PageResponse<Merchant>> listPending(MerchantQueryRequest request) {
        request.setAuditStatus(0);
        PageResponse<Merchant> page = merchantService.listMerchants(request);
        return Result.success(page);
    }

    @Operation(summary = "商家审核")
    @PutMapping("/audit")
    public Result<Void> audit(@Valid @RequestBody MerchantAuditRequest request) {
        merchantService.audit(request);
        return Result.success();
    }

    @Operation(summary = "查询商家列表")
    @GetMapping("/list")
    public Result<PageResponse<Merchant>> listMerchants(MerchantQueryRequest request) {
        PageResponse<Merchant> page = merchantService.listMerchants(request);
        return Result.success(page);
    }

    @Operation(summary = "获取商家详情")
    @GetMapping("/{id}")
    public Result<Merchant> getMerchantById(@PathVariable Long id) {
        Merchant merchant = merchantService.getMerchantById(id);
        if (merchant == null) {
            return Result.fail("商家不存在");
        }
        return Result.success(merchant);
    }

    @Operation(summary = "获取商家统计信息")
    @GetMapping("/stats")
    public Result<Map<String, Object>> getStats() {
        Map<String, Object> stats = new HashMap<>();
        
        MerchantQueryRequest pendingRequest = new MerchantQueryRequest();
        pendingRequest.setAuditStatus(0);
        pendingRequest.setPageNum(1);
        pendingRequest.setPageSize(1);
        stats.put("pendingCount", merchantService.listMerchants(pendingRequest).getTotal());

        MerchantQueryRequest approvedRequest = new MerchantQueryRequest();
        approvedRequest.setAuditStatus(2);
        approvedRequest.setPageNum(1);
        approvedRequest.setPageSize(1);
        stats.put("approvedCount", merchantService.listMerchants(approvedRequest).getTotal());

        stats.put("totalCount", merchantService.listMerchants(new MerchantQueryRequest()).getTotal());

        return Result.success(stats);
    }

    @Operation(summary = "审核资质")
    @PutMapping("/qualification/audit")
    public Result<Void> auditQualification(@Valid @RequestBody QualificationAuditRequest request) {
        merchantQualificationService.auditQualification(request);
        return Result.success();
    }

    @Operation(summary = "查询商家资质列表")
    @GetMapping("/qualification/list/{merchantId}")
    public Result<List<MerchantQualification>> listQualifications(@PathVariable Long merchantId) {
        List<MerchantQualification> qualifications = merchantQualificationService.listQualifications(merchantId);
        return Result.success(qualifications);
    }
}
