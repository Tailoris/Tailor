package com.tailoris.admin.controller;

import com.tailoris.admin.service.AdminRoleService;
import com.tailoris.common.dto.PageResponse;
import com.tailoris.common.result.Result;
import cn.dev33.satoken.annotation.SaCheckRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "平台角色管理")
@RestController
@RequestMapping("/api/admin/roles")
@RequiredArgsConstructor
@SaCheckRole("admin")
public class AdminRoleController {

    private final AdminRoleService adminRoleService;

    @Operation(summary = "分页查询角色列表")
    @GetMapping
    public Result<PageResponse<Map<String, Object>>> listRoles(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword) {
        PageResponse<Map<String, Object>> page = adminRoleService.listRoles(keyword, current, size);
        return Result.success(page);
    }

    @Operation(summary = "获取角色详情")
    @GetMapping("/{id}")
    public Result<Map<String, Object>> getRoleDetail(@PathVariable Long id) {
        Map<String, Object> role = adminRoleService.getRoleDetail(id);
        if (role == null) {
            return Result.fail("角色不存在");
        }
        return Result.success(role);
    }

    @Operation(summary = "新增角色")
    @PostMapping
    public Result<Map<String, Object>> createRole(@RequestBody Map<String, Object> role) {
        Map<String, Object> result = adminRoleService.createRole(role);
        return Result.success(result);
    }

    @Operation(summary = "更新角色")
    @PutMapping("/{id}")
    public Result<Map<String, Object>> updateRole(@PathVariable Long id, @RequestBody Map<String, Object> role) {
        Map<String, Object> result = adminRoleService.updateRole(id, role);
        if (result == null) {
            return Result.fail("角色不存在");
        }
        return Result.success(result);
    }

    @Operation(summary = "删除角色")
    @DeleteMapping("/{id}")
    public Result<Void> deleteRole(@PathVariable Long id) {
        adminRoleService.deleteRole(id);
        return Result.success();
    }

    @Operation(summary = "获取角色权限列表")
    @GetMapping("/{id}/permissions")
    public Result<List<Map<String, Object>>> getRolePermissions(@PathVariable Long id) {
        List<Map<String, Object>> permissions = adminRoleService.getRolePermissions(id);
        return Result.success(permissions);
    }

    @Operation(summary = "分配角色权限")
    @PutMapping("/{id}/permissions")
    public Result<Void> assignRolePermissions(@PathVariable Long id, @RequestBody Map<String, Long[]> body) {
        Long[] permissionIds = body.get("permissionIds");
        adminRoleService.assignRolePermissions(id, permissionIds);
        return Result.success();
    }

    @Operation(summary = "获取角色关联用户列表")
    @GetMapping("/{id}/users")
    public Result<PageResponse<Map<String, Object>>> getRoleUsers(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size) {
        PageResponse<Map<String, Object>> page = adminRoleService.getRoleUsers(id, current, size);
        return Result.success(page);
    }

    @Operation(summary = "获取所有角色（用于下拉选择）")
    @GetMapping("/all")
    public Result<List<Map<String, Object>>> listAllRoles() {
        List<Map<String, Object>> roles = adminRoleService.listAllRoles();
        return Result.success(roles);
    }
}