package com.tailoris.admin.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaCheckRole;
import com.tailoris.api.admin.dto.MerchantModifyApplyAuditRequest;
import com.tailoris.api.admin.dto.MerchantModifyApplyQueryRequest;
import com.tailoris.common.constant.RoutePortConstant;
import com.tailoris.admin.service.AdminMerchantModifyService;
import com.tailoris.common.dto.PageResponse;
import com.tailoris.common.result.Result;
import com.tailoris.api.merchant.entity.MerchantModifyApply;
import com.tailoris.common.util.AdminSecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@SaCheckRole({"SUPER_ADMIN", "PLATFORM_ADMIN"})
@Tag(name = "平台商家管理-商家修改申请审核")
@RestController
@RequestMapping(RoutePortConstant.ROUTE_ADMIN_MERCHANT_MODIFY)
@RequiredArgsConstructor
public class AdminMerchantModifyController {

    private final AdminMerchantModifyService adminMerchantModifyService;

    @SaCheckPermission("merchant:modify:view")
    @Operation(summary = "分页查询商家修改申请")
    @GetMapping("/page")
    public Result<PageResponse<MerchantModifyApply>> page(MerchantModifyApplyQueryRequest req) {
        return Result.success(adminMerchantModifyService.page(req));
    }

    @SaCheckPermission("merchant:modify:view")
    @Operation(summary = "获取商家修改申请详情")
    @GetMapping("/{id}")
    public Result<MerchantModifyApply> detail(@PathVariable Long id) {
        return Result.success(adminMerchantModifyService.detail(id));
    }

    @SaCheckPermission("merchant:modify:audit")
    @Operation(summary = "审核商家修改申请")
    @PutMapping("/audit")
    public Result<Void> audit(@Valid @RequestBody MerchantModifyApplyAuditRequest req) {
        adminMerchantModifyService.audit(req, AdminSecurityUtils.getLoginIdAsLong());
        return Result.success();
    }
}
