package com.tailoris.admin.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.tailoris.admin.dto.PatternAuditRequest;
import com.tailoris.common.constant.RoutePortConstant;
import com.tailoris.admin.dto.PatternQueryRequest;
import com.tailoris.admin.dto.PatternScoreRequest;
import com.tailoris.admin.service.AdminPatternService;
import com.tailoris.common.dto.PageResponse;
import com.tailoris.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@SaCheckRole({"SUPER_ADMIN", "PLATFORM_ADMIN", "DESIGN_ADMIN"})
@Tag(name = "纸样设计管理")
@RestController
@RequestMapping(RoutePortConstant.ROUTE_ADMIN_PATTERN)
@RequiredArgsConstructor
public class AdminPatternController {

    private final AdminPatternService adminPatternService;

    @Operation(summary = "查询纸样列表")
    @GetMapping
    public Result<PageResponse<Object>> listPatterns(PatternQueryRequest request) {
        PageResponse<Object> page = adminPatternService.listPatterns(request);
        return Result.success(page);
    }

    @Operation(summary = "获取纸样详情")
    @GetMapping("/{id}")
    public Result<Object> getPattern(@Parameter(description = "纸样ID") @PathVariable Long id) {
        Object pattern = adminPatternService.getPattern(id);
        return Result.success(pattern);
    }

    @Operation(summary = "审核纸样")
    @PutMapping("/{id}/audit")
    public Result<Void> auditPattern(
            @Parameter(description = "纸样ID") @PathVariable Long id,
            @Valid @RequestBody PatternAuditRequest request) {
        adminPatternService.auditPattern(id, request);
        return Result.success();
    }

    @Operation(summary = "更新纸样评分")
    @PutMapping("/{id}/score")
    public Result<Void> updatePatternScore(
            @Parameter(description = "纸样ID") @PathVariable Long id,
            @Valid @RequestBody PatternScoreRequest request) {
        adminPatternService.updatePatternScore(id, request);
        return Result.success();
    }

    @Operation(summary = "删除纸样")
    @DeleteMapping("/{id}")
    public Result<Void> deletePattern(@Parameter(description = "纸样ID") @PathVariable Long id) {
        adminPatternService.deletePattern(id);
        return Result.success();
    }
}