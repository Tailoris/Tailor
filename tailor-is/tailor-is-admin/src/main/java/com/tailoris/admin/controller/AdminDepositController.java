package com.tailoris.admin.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaCheckRole;
import com.tailoris.api.admin.dto.DepositAdjustRequest;
import com.tailoris.api.admin.dto.DepositFlowQueryRequest;
import com.tailoris.api.admin.dto.DepositQueryRequest;
import com.tailoris.api.merchant.entity.DepositFlow;
import com.tailoris.common.constant.RoutePortConstant;
import com.tailoris.admin.service.AdminDepositService;
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
@Tag(name = "平台商家管理-保障金管理")
@RestController
@RequestMapping(RoutePortConstant.ROUTE_ADMIN_DEPOSIT)
@RequiredArgsConstructor
public class AdminDepositController {

    private final AdminDepositService adminDepositService;

    @SaCheckPermission("merchant:deposit:view")
    @Operation(summary = "分页查询保障金记录")
    @GetMapping("/page")
    public Result<PageResponse<Map<String, Object>>> page(DepositQueryRequest req) {
        return Result.success(adminDepositService.page(req));
    }

    @SaCheckPermission("merchant:deposit:view")
    @Operation(summary = "获取商家保障金详情（含质保金记录+流水）")
    @GetMapping("/detail/{merchantId}")
    public Result<Map<String, Object>> getMerchantDepositDetail(@PathVariable Long merchantId) {
        return Result.success(adminDepositService.getMerchantDepositDetail(merchantId));
    }

    @SaCheckPermission("merchant:deposit:view")
    @Operation(summary = "分页查询保障金流水")
    @GetMapping("/flow")
    public Result<PageResponse<DepositFlow>> flowPage(DepositFlowQueryRequest req) {
        return Result.success(adminDepositService.flowPage(req));
    }

    @SaCheckPermission("merchant:deposit:edit")
    @Operation(summary = "调整保障金（1补缴/2调增/3调减/4退还）")
    @PostMapping("/adjust")
    public Result<Void> adjustDeposit(@Valid @RequestBody DepositAdjustRequest req) {
        adminDepositService.adjustDeposit(req, AdminSecurityUtils.getLoginIdAsLong());
        return Result.success();
    }
}
