package com.tailoris.merchant.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckRole;
import com.tailoris.common.result.Result;
import com.tailoris.merchant.entity.MerchantTrialAssessment;
import com.tailoris.merchant.service.IMerchantTrialAssessmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 商家试运营考核控制器 - MER-006.
 *
 * @author Tailor IS Team
 * @since 2026-06-03
 */
@Tag(name = "Merchant Trial Assessment API", description = "商家试运营考核接口 - MER-006")
@RestController
@RequestMapping("/api/v1/merchant/trial")
@RequiredArgsConstructor
@Validated
public class MerchantTrialAssessmentController {

    private final IMerchantTrialAssessmentService trialService;

    @Operation(summary = "为新商家创建试运营记录（注册时自动调用）")
    @PostMapping("/create")
    @SaCheckRole("admin")
    public Result<MerchantTrialAssessment> create(@RequestParam Long merchantId) {
        return Result.success(trialService.createTrialRecord(merchantId));
    }

    @Operation(summary = "执行试运营考核")
    @PostMapping("/assess")
    @SaCheckRole("admin")
    public Result<MerchantTrialAssessment> assess(@RequestParam Long merchantId) {
        return Result.success(trialService.performAssessment(merchantId));
    }

    @Operation(summary = "商家转正")
    @PostMapping("/promote")
    @SaCheckRole("admin")
    public Result<Boolean> promote(@RequestParam Long merchantId, @RequestParam(required = false) String remark) {
        return Result.success(trialService.promote(merchantId, remark));
    }

    @Operation(summary = "延期考核")
    @PostMapping("/extend")
    @SaCheckRole("admin")
    public Result<Boolean> extend(
            @RequestParam Long merchantId,
            @RequestParam(defaultValue = "30") int additionalDays,
            @RequestParam(required = false) String remark) {
        return Result.success(trialService.extend(merchantId, additionalDays, remark));
    }

    @Operation(summary = "考核不通过关闭店铺")
    @PostMapping("/fail")
    @SaCheckRole("admin")
    public Result<Boolean> failAndClose(@RequestParam Long merchantId, @RequestParam(required = false) String remark) {
        return Result.success(trialService.failAndClose(merchantId, remark));
    }

    @Operation(summary = "查询待考核商家列表（管理员）")
    @GetMapping("/pending")
    @SaCheckRole("admin")
    public Result<List<MerchantTrialAssessment>> listPending() {
        return Result.success(trialService.listPendingAssessments());
    }

    @Operation(summary = "查询商家考核历史")
    @GetMapping("/history/{merchantId}")
    @SaCheckLogin
    public Result<List<MerchantTrialAssessment>> history(@PathVariable Long merchantId) {
        return Result.success(trialService.getAssessmentHistory(merchantId));
    }
}
