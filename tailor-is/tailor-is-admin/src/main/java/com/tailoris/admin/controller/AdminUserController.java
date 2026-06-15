package com.tailoris.admin.controller;

import com.tailoris.api.admin.dto.UserQueryRequest;
import com.tailoris.admin.service.AdminUserService;
import com.tailoris.common.dto.PageResponse;
import com.tailoris.common.result.Result;
import com.tailoris.api.user.entity.SysUser;
import cn.dev33.satoken.annotation.SaCheckRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "平台用户管理")
@RestController
@RequestMapping("/api/admin/user")
@RequiredArgsConstructor
@SaCheckRole("admin")
public class AdminUserController {

    private final AdminUserService adminUserService;

    @Operation(summary = "查询用户列表")
    @GetMapping("/list")
    public Result<PageResponse<SysUser>> listUsers(UserQueryRequest request) {
        PageResponse<SysUser> page = adminUserService.listUsers(request);
        return Result.success(page);
    }

    @Operation(summary = "获取用户详情")
    @GetMapping("/{id}")
    public Result<SysUser> getUserDetail(@PathVariable Long id) {
        SysUser user = adminUserService.getUserDetail(id);
        return Result.success(user);
    }

    @Operation(summary = "冻结用户")
    @PutMapping("/freeze/{id}")
    public Result<Void> freezeUser(@PathVariable Long id) {
        adminUserService.freezeUser(id);
        return Result.success();
    }

    @Operation(summary = "解冻用户")
    @PutMapping("/unfreeze/{id}")
    public Result<Void> unfreezeUser(@PathVariable Long id) {
        adminUserService.unfreezeUser(id);
        return Result.success();
    }
}
