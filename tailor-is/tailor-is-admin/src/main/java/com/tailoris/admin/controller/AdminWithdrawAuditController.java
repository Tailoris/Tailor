package com.tailoris.admin.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaCheckRole;
import com.tailoris.api.admin.dto.WithdrawAuditRequest;
import com.tailoris.common.constant.RoutePortConstant;
import com.tailoris.admin.service.AdminWithdrawAuditService;
import com.tailoris.common.dto.PageResponse;
import com.tailoris.common.result.Result;
import com.tailoris.common.util.AdminSecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@SaCheckRole({"SUPER_ADMIN", "PLATFORM_ADMIN"})
@Tag(name = "平台商家管理-商家提现审核")
@RestController
@RequestMapping(RoutePortConstant.ROUTE_ADMIN_WITHDRAW_AUDIT)
@RequiredArgsConstructor
public class AdminWithdrawAuditController {

    private final AdminWithdrawAuditService adminWithdrawAuditService;

    @SaCheckPermission("merchant:withdraw:view")
    @Operation(summary = "分页查询提现申请")
    @GetMapping("/page")
    public Result<PageResponse<Map<String, Object>>> page(
            @RequestParam(required = false) String merchantName,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size) {
        return Result.success(adminWithdrawAuditService.page(merchantName, status, startDate, endDate, current, size));
    }

    @SaCheckPermission("merchant:withdraw:view")
    @Operation(summary = "获取提现申请详情")
    @GetMapping("/{id}")
    public Result<Map<String, Object>> getDetail(@PathVariable Long id) {
        return Result.success(adminWithdrawAuditService.getDetail(id));
    }

    @SaCheckPermission("merchant:withdraw:audit")
    @Operation(summary = "审核提现申请（通过status=2/驳回status=4）")
    @PutMapping("/audit")
    public Result<Void> audit(@Valid @RequestBody WithdrawAuditRequest req) {
        adminWithdrawAuditService.audit(req, AdminSecurityUtils.getLoginIdAsLong());
        return Result.success();
    }
}
