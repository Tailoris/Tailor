package com.tailoris.user.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.stp.StpUtil;
import com.tailoris.common.exception.BusinessException;
import com.tailoris.common.result.Result;
import com.tailoris.user.service.SysRoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 角色管理控制器 - 修复 TD-CR3
 *
 * <p>提供角色分配、移除等管理功能。
 * 严格权限校验：仅超级管理员可操作，且操作人不能修改自己。</p>
 *
 * <h3>修复要点</h3>
 * <ul>
 *   <li>TD-CR3: 严格权限校验，操作者必须是admin角色</li>
 *   <li>防止自我权限提升：不能修改自己的角色</li>
 *   <li>参数级校验：用户ID/角色ID必须为正整数</li>
 *   <li>操作审计日志：所有敏感操作记录</li>
 * </ul>
 *
 * @author Tailor IS Team
 * @since 1.0.0
 */
@Slf4j
@Validated
@Tag(name = "角色管理", description = "用户角色分配、移除等管理接口")
@RestController
@RequestMapping("/api/user/roles")
@RequiredArgsConstructor
public class RoleController {

    private final SysRoleService roleService;

    @Operation(summary = "为用户分配角色")
    @SaCheckRole("admin")
    @PostMapping("/{userId}")
    public Result<Void> assignRoleToUser(
            @Parameter(description = "用户ID", example = "1")
            @PathVariable @NotNull @Min(value = 1, message = "用户ID必须为正数") Long userId,
            @Parameter(description = "角色ID", example = "1")
            @RequestParam @NotNull @Min(value = 1, message = "角色ID必须为正数") Long roleId) {
        // 🔒 TD-CR3: 防止自我提权
        Long currentUserId = StpUtil.getLoginIdAsLong();
        if (currentUserId.equals(userId)) {
            log.warn("用户尝试修改自己的角色: operatorId={}, targetUserId={}", currentUserId, userId);
            throw new BusinessException("禁止修改自己的角色，请联系其他超级管理员操作");
        }

        log.info("管理员分配角色: operatorId={}, targetUserId={}, roleId={}", currentUserId, userId, roleId);
        roleService.assignRoleToUser(userId, roleId);
        return Result.success();
    }

    @Operation(summary = "移除用户的角色")
    @SaCheckRole("admin")
    @DeleteMapping("/{userId}")
    public Result<Void> removeRoleFromUser(
            @Parameter(description = "用户ID", example = "1")
            @PathVariable @NotNull @Min(value = 1, message = "用户ID必须为正数") Long userId,
            @Parameter(description = "角色ID", example = "1")
            @RequestParam @NotNull @Min(value = 1, message = "角色ID必须为正数") Long roleId) {
        // 🔒 TD-CR3: 防止自我提权
        Long currentUserId = StpUtil.getLoginIdAsLong();
        if (currentUserId.equals(userId)) {
            log.warn("用户尝试移除自己的角色: operatorId={}, targetUserId={}", currentUserId, userId);
            throw new BusinessException("禁止修改自己的角色，请联系其他超级管理员操作");
        }

        log.info("管理员移除角色: operatorId={}, targetUserId={}, roleId={}", currentUserId, userId, roleId);
        roleService.removeRoleFromUser(userId, roleId);
        return Result.success();
    }
}
