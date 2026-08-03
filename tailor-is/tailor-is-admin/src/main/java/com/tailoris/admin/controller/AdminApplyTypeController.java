package com.tailoris.admin.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaCheckRole;
import com.tailoris.api.admin.dto.ApplyTypeDTO;
import com.tailoris.api.admin.dto.ApplyTypeQueryRequest;
import com.tailoris.common.constant.RoutePortConstant;
import com.tailoris.admin.service.AdminApplyTypeService;
import com.tailoris.common.dto.PageResponse;
import com.tailoris.common.result.Result;
import com.tailoris.api.merchant.entity.ApplyType;
import com.tailoris.common.util.AdminSecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@SaCheckRole({"SUPER_ADMIN", "PLATFORM_ADMIN"})
@Tag(name = "平台商家管理-入驻类型配置")
@RestController
@RequestMapping(RoutePortConstant.ROUTE_ADMIN_APPLY_TYPE)
@RequiredArgsConstructor
public class AdminApplyTypeController {

    private final AdminApplyTypeService adminApplyTypeService;

    @SaCheckPermission("merchant:applytype:view")
    @Operation(summary = "分页查询入驻类型配置")
    @GetMapping("/page")
    public Result<PageResponse<ApplyType>> page(ApplyTypeQueryRequest req) {
        return Result.success(adminApplyTypeService.page(req));
    }

    @SaCheckPermission("merchant:applytype:view")
    @Operation(summary = "获取启用的入驻类型列表")
    @GetMapping("/enabled")
    public Result<List<ApplyType>> listEnabled() {
        return Result.success(adminApplyTypeService.listEnabled());
    }

    @SaCheckPermission("merchant:applytype:view")
    @Operation(summary = "获取入驻类型详情")
    @GetMapping("/{id}")
    public Result<ApplyType> getById(@PathVariable Long id) {
        return Result.success(adminApplyTypeService.getById(id));
    }

    @SaCheckPermission("merchant:applytype:add")
    @Operation(summary = "新增入驻类型配置")
    @PostMapping
    public Result<Void> create(@Valid @RequestBody ApplyTypeDTO dto) {
        adminApplyTypeService.create(dto, AdminSecurityUtils.getLoginIdAsLong());
        return Result.success();
    }

    @SaCheckPermission("merchant:applytype:edit")
    @Operation(summary = "修改入驻类型配置")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody ApplyTypeDTO dto) {
        adminApplyTypeService.update(id, dto, AdminSecurityUtils.getLoginIdAsLong());
        return Result.success();
    }

    @SaCheckPermission("merchant:applytype:delete")
    @Operation(summary = "删除入驻类型配置")
    @DeleteMapping("/{id}")
    public Result<Void> remove(@PathVariable Long id) {
        adminApplyTypeService.remove(id, AdminSecurityUtils.getLoginIdAsLong());
        return Result.success();
    }
}
