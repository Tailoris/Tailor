package com.tailoris.merchant.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckRole;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tailoris.common.annotation.RateLimit;
import com.tailoris.common.result.Result;
import com.tailoris.merchant.dto.ViolationAppealRequest;
import com.tailoris.merchant.dto.ViolationPunishRequest;
import com.tailoris.merchant.dto.ViolationReportRequest;
import com.tailoris.merchant.entity.MerchantViolation;
import com.tailoris.merchant.service.IMerchantViolationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 商家违规处罚控制器 - MER-007.
 *
 * @author Tailor IS Team
 * @since 2026-06-03
 */
@Tag(name = "Merchant Violation API", description = "商家违规处罚接口 - MER-007")
@RestController
@RequestMapping("/api/v1/merchant/violation")
@RequiredArgsConstructor
@Validated
public class MerchantViolationController {

    private final IMerchantViolationService violationService;

    @Operation(summary = "提交违规举报（用户）")
    @PostMapping("/report")
    @SaCheckLogin
    @RateLimit(key = "merchant_violation_report", permitsPerSecond = 2)
    public Result<MerchantViolation> report(@RequestBody @Validated ViolationReportRequest request) {
        return Result.success(violationService.report(request));
    }

    @Operation(summary = "执行处罚（管理员）")
    @PostMapping("/punish")
    @SaCheckRole("admin")
    public Result<MerchantViolation> punish(@RequestBody @Validated ViolationPunishRequest request) {
        return Result.success(violationService.punish(request));
    }

    @Operation(summary = "商家申诉")
    @PostMapping("/appeal")
    @SaCheckLogin
    public Result<MerchantViolation> appeal(@RequestBody @Validated ViolationAppealRequest request) {
        return Result.success(violationService.appeal(request));
    }

    @Operation(summary = "处理申诉（管理员）")
    @PostMapping("/appeal/handle")
    @SaCheckRole("admin")
    public Result<MerchantViolation> handleAppeal(
            @Parameter(description = "违规ID") @RequestParam Long violationId,
            @Parameter(description = "是否通过申诉") @RequestParam boolean approved,
            @Parameter(description = "处理结果") @RequestParam String result,
            @Parameter(description = "处理人ID") @RequestParam Long handlerId) {
        return Result.success(violationService.handleAppeal(violationId, approved, result, handlerId));
    }

    @Operation(summary = "撤销违规（管理员）")
    @PostMapping("/revoke")
    @SaCheckRole("admin")
    public Result<Boolean> revoke(
            @RequestParam Long violationId,
            @RequestParam String reason,
            @RequestParam Long handlerId) {
        return Result.success(violationService.revoke(violationId, reason, handlerId));
    }

    @Operation(summary = "解除处罚")
    @PostMapping("/release")
    @SaCheckRole("admin")
    public Result<Boolean> release(@RequestParam Long violationId) {
        return Result.success(violationService.release(violationId));
    }

    @Operation(summary = "查询商家违规记录")
    @GetMapping("/list/merchant/{merchantId}")
    @SaCheckLogin
    public Result<List<MerchantViolation>> listByMerchant(
            @PathVariable Long merchantId,
            @RequestParam(required = false) Integer status) {
        List<MerchantViolation> all = violationService.listByMerchant(merchantId);
        if (status != null) {
            return Result.success(all.stream()
                    .filter(v -> status.equals(v.getStatus()))
                    .toList());
        }
        return Result.success(all);
    }

    @Operation(summary = "分页查询违规记录（管理员）")
    @GetMapping("/page")
    @SaCheckRole("admin")
    public Result<IPage<MerchantViolation>> page(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) Long merchantId,
            @RequestParam(required = false) Integer violationType,
            @RequestParam(required = false) Integer status) {
        Page<MerchantViolation> page = new Page<>(pageNum, pageSize);
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<MerchantViolation> wrapper
                = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        if (merchantId != null) {
            wrapper.eq(MerchantViolation::getMerchantId, merchantId);
        }
        if (violationType != null) {
            wrapper.eq(MerchantViolation::getViolationType, violationType);
        }
        if (status != null) {
            wrapper.eq(MerchantViolation::getStatus, status);
        }
        wrapper.orderByDesc(MerchantViolation::getCreateTime);
        return Result.success(violationService.page(page, wrapper));
    }

    @Operation(summary = "获取商家违规统计")
    @GetMapping("/stats/{merchantId}")
    @SaCheckLogin
    public Result<Map<String, Object>> stats(@PathVariable Long merchantId) {
        return Result.success(violationService.getViolationStats(merchantId));
    }

    @Operation(summary = "检查商家是否被封禁")
    @GetMapping("/check/banned/{merchantId}")
    public Result<Boolean> isBanned(@PathVariable Long merchantId) {
        return Result.success(violationService.isBanned(merchantId));
    }

    @Operation(summary = "检查商家是否被限流")
    @GetMapping("/check/limited/{merchantId}")
    public Result<Boolean> isLimited(@PathVariable Long merchantId) {
        return Result.success(violationService.isLimited(merchantId));
    }
}
