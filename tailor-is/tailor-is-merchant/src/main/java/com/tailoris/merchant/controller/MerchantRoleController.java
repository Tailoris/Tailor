package com.tailoris.merchant.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckRole;
import com.tailoris.common.result.Result;
import com.tailoris.merchant.entity.MerchantRoleTemplate;
import com.tailoris.merchant.service.IMerchantPermissionService;
import com.tailoris.merchant.service.IMerchantRoleTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 商家角色权限控制器 - MER-002.
 *
 * @author Tailor IS Team
 * @since 2026-06-03
 */
@Tag(name = "Merchant Role API", description = "商家角色权限接口 - MER-002")
@RestController
@RequestMapping("/api/v1/merchant/role")
@RequiredArgsConstructor
@Validated
public class MerchantRoleController {

    private final IMerchantRoleTemplateService roleTemplateService;
    private final IMerchantPermissionService permissionService;

    @Operation(summary = "获取系统预设角色")
    @GetMapping("/system")
    public Result<List<MerchantRoleTemplate>> listSystemRoles() {
        return Result.success(roleTemplateService.listSystemRoles());
    }

    @Operation(summary = "获取商家自定义角色")
    @GetMapping("/merchant/{merchantId}")
    @SaCheckLogin
    public Result<List<MerchantRoleTemplate>> listMerchantRoles(@PathVariable Long merchantId) {
        return Result.success(roleTemplateService.listMerchantRoles(merchantId));
    }

    @Operation(summary = "获取员工权限（按钮级）")
    @GetMapping("/permissions/{employeeId}")
    @SaCheckLogin
    public Result<Set<String>> getEmployeePermissions(@PathVariable Long employeeId) {
        return Result.success(permissionService.getEmployeePermissions(employeeId));
    }

    @Operation(summary = "校验员工是否拥有某权限")
    @GetMapping("/check")
    @SaCheckLogin
    public Result<Map<String, Boolean>> checkPermission(
            @RequestParam Long employeeId,
            @RequestParam String codes) {
        String[] arr = codes.split(",");
        boolean[] results = permissionService.hasPermissions(employeeId, arr);
        Map<String, Boolean> map = new HashMap<>();
        for (int i = 0; i < arr.length; i++) {
            map.put(arr[i].trim(), results[i]);
        }
        return Result.success(map);
    }

    @Operation(summary = "创建商家自定义角色")
    @PostMapping("/create")
    @SaCheckRole("merchant_owner")
    public Result<MerchantRoleTemplate> createRole(
            @RequestParam Long merchantId,
            @RequestParam String roleName,
            @RequestParam(required = false) String description,
            @RequestBody List<String> permissions) {
        return Result.success(roleTemplateService.createMerchantRole(
                merchantId, roleName, permissions, description));
    }

    @Operation(summary = "更新角色权限")
    @PutMapping("/{roleId}/permissions")
    @SaCheckRole("merchant_owner")
    public Result<Boolean> updatePermissions(
            @PathVariable Long roleId,
            @RequestBody List<String> permissions) {
        return Result.success(roleTemplateService.updatePermissions(roleId, permissions));
    }

    @Operation(summary = "删除自定义角色")
    @DeleteMapping("/{roleId}")
    @SaCheckRole("merchant_owner")
    public Result<Boolean> deleteRole(
            @PathVariable Long roleId,
            @RequestParam Long merchantId) {
        return Result.success(roleTemplateService.deleteRole(roleId, merchantId));
    }
}
