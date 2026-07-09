package com.tailoris.admin.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.tailoris.admin.dto.CopyrightAuditRequest;
import com.tailoris.common.constant.RoutePortConstant;
import com.tailoris.admin.dto.CopyrightQueryRequest;
import com.tailoris.admin.service.AdminCopyrightService;
import com.tailoris.common.dto.PageResponse;
import com.tailoris.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@SaCheckRole({"SUPER_ADMIN", "PLATFORM_ADMIN", "COPYRIGHT_ADMIN"})
@Tag(name = "版权管理")
@RestController
@RequestMapping(RoutePortConstant.ROUTE_ADMIN_COPYRIGHT)
@RequiredArgsConstructor
public class AdminCopyrightController {

    private final AdminCopyrightService adminCopyrightService;

    @Operation(summary = "查询版权列表")
    @GetMapping
    public Result<PageResponse<Object>> listCopyrights(CopyrightQueryRequest request) {
        PageResponse<Object> page = adminCopyrightService.listCopyrights(request);
        return Result.success(page);
    }

    @Operation(summary = "获取版权详情")
    @GetMapping("/{id}")
    public Result<Object> getCopyright(@Parameter(description = "版权ID") @PathVariable Long id) {
        Object copyright = adminCopyrightService.getCopyright(id);
        return Result.success(copyright);
    }

    @Operation(summary = "审核版权")
    @PutMapping("/{id}/audit")
    public Result<Void> auditCopyright(
            @Parameter(description = "版权ID") @PathVariable Long id,
            @Valid @RequestBody CopyrightAuditRequest request) {
        adminCopyrightService.auditCopyright(id, request);
        return Result.success();
    }

    @Operation(summary = "执行侵权检测")
    @PostMapping("/{id}/detect")
    public Result<Void> detectInfringement(@Parameter(description = "版权ID") @PathVariable Long id) {
        adminCopyrightService.detectInfringement(id);
        return Result.success();
    }

    @Operation(summary = "查询存证记录")
    @GetMapping("/{id}/evidence")
    public Result<PageResponse<Object>> getEvidenceRecords(@Parameter(description = "版权ID") @PathVariable Long id) {
        PageResponse<Object> records = adminCopyrightService.getEvidenceRecords(id);
        return Result.success(records);
    }
}