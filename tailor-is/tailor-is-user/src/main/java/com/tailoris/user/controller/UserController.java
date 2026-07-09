package com.tailoris.user.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.stp.StpUtil;
import com.tailoris.common.dto.PageRequest;
import com.tailoris.common.dto.PageResponse;
import com.tailoris.common.exception.BusinessException;
import com.tailoris.common.result.Result;
import com.tailoris.user.dto.LoginResponse;
import com.tailoris.user.dto.PasswordUpdateRequest;
import com.tailoris.user.dto.RealNameAuthRequest;
import com.tailoris.user.dto.UserUpdateRequest;
import com.tailoris.user.entity.SysUser;
import com.tailoris.user.service.SysUserService;
import com.tailoris.common.annotation.ApiVersion;
import com.tailoris.common.constant.RoutePortConstant;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@ApiVersion(value = "1.0", deprecated = true, deprecatedSince = "1.1.0", replacement = "/api/v1/user")
@SaCheckLogin
@Tag(name = "用户管理", description = "用户信息管理接口")
@RestController
@RequestMapping(RoutePortConstant.ROUTE_USER)
@RequiredArgsConstructor
public class UserController {

    private final SysUserService sysUserService;

    @Operation(summary = "获取当前用户信息", description = "获取当前登录用户的详细信息")
    @GetMapping("/info")
    public Result<LoginResponse.UserInfo> getUserInfo() {
        if (!StpUtil.isLogin()) {
            throw new BusinessException("用户未登录");
        }
        Long userId = StpUtil.getLoginIdAsLong();
        LoginResponse.UserInfo userInfo = sysUserService.getUserInfo(userId);
        return Result.success(userInfo);
    }

    @Operation(summary = "更新用户信息", description = "更新当前用户的昵称、头像、性别、生日")
    @PutMapping("/info")
    public Result<Void> updateUserInfo(@Valid @RequestBody UserUpdateRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        sysUserService.updateUser(userId, request);
        return Result.success();
    }

    @Operation(summary = "实名认证", description = "提交真实姓名和身份证信息进行实名认证")
    @PutMapping("/real-name-auth")
    public Result<Void> realNameAuth(@Valid @RequestBody RealNameAuthRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        sysUserService.realNameAuth(userId, request);
        return Result.success();
    }

    @SaCheckRole("admin")
    @Operation(summary = "用户列表", description = "分页查询用户列表（管理端）")
    @GetMapping("/list")
    public Result<PageResponse<SysUser>> listUsers(PageRequest pageRequest) {
        PageResponse<SysUser> response = sysUserService.listUsers(pageRequest);
        return Result.success(response);
    }

    @Operation(summary = "更新用户资料", description = "更新当前用户的昵称、头像、性别、生日、邮箱")
    @PutMapping("/profile")
    public Result<Void> updateProfile(@Valid @RequestBody UserUpdateRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        sysUserService.updateUser(userId, request);
        return Result.success();
    }

    @Operation(summary = "修改密码", description = "验证旧密码后修改为新密码")
    @PutMapping("/password")
    public Result<Void> updatePassword(@Valid @RequestBody PasswordUpdateRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        sysUserService.updatePassword(userId, request.getOldPassword(), request.getNewPassword());
        return Result.success();
    }
}
