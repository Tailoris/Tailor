package com.tailoris.admin.controller;

import com.tailoris.admin.service.AdminUserService;
import com.tailoris.common.constant.RoutePortConstant;
import com.tailoris.common.dto.PageResponse;
import com.tailoris.common.result.Result;
import com.tailoris.api.user.entity.SysUser;
import cn.dev33.satoken.annotation.SaCheckRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Tag(name = "平台用户管理")
@RestController
@RequestMapping(RoutePortConstant.ROUTE_ADMIN_USERS)
@RequiredArgsConstructor
@SaCheckRole({"SUPER_ADMIN", "PLATFORM_ADMIN"})
public class AdminUserController {

    private final AdminUserService adminUserService;
    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;

    @Operation(summary = "查询用户列表")
    @GetMapping
    public Result<PageResponse<Map<String, Object>>> listUsers(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {
        com.tailoris.api.admin.dto.UserQueryRequest request = new com.tailoris.api.admin.dto.UserQueryRequest();
        request.setPageNum(current);
        request.setPageSize(size);
        request.setKeyword(keyword);
        if (status != null && !status.isEmpty()) {
            request.setStatus(Integer.parseInt(status));
        }
        PageResponse<SysUser> page = adminUserService.listUsers(request);
        
        List<Map<String, Object>> recordsWithRoles = new ArrayList<>();
        for (SysUser user : page.getRecords()) {
            Map<String, Object> userMap = convertUserToMap(user);
            recordsWithRoles.add(userMap);
        }
        
        return Result.success(new PageResponse<>(recordsWithRoles, page.getTotal(), current, size));
    }

    @Operation(summary = "获取用户详情")
    @GetMapping("/{id}")
    public Result<Map<String, Object>> getUserDetail(@PathVariable Long id) {
        SysUser user = adminUserService.getUserDetail(id);
        if (user == null) {
            return Result.fail("用户不存在");
        }
        return Result.success(convertUserToMap(user));
    }

    @Operation(summary = "创建用户")
    @PostMapping
    public Result<Map<String, Object>> createUser(@RequestBody Map<String, Object> userData) {
        try {
            String sql = "INSERT INTO tailor_is_user.sys_user (username, password, real_name, phone, email, status, create_time, update_time) " +
                    "VALUES (?, ?, ?, ?, ?, ?, NOW(), NOW())";
            String encodedPassword = passwordEncoder.encode(userData.get("password") != null ? userData.get("password").toString() : "123456");
            jdbcTemplate.update(sql,
                    userData.get("username"),
                    encodedPassword,
                    userData.get("realName"),
                    userData.get("phone"),
                    userData.get("email"),
                    userData.get("status"));
            
            Long id = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
            SysUser user = adminUserService.getUserDetail(id);
            return Result.success(convertUserToMap(user));
        } catch (Exception e) {
            log.error("Failed to create user", e);
            return Result.fail("创建用户失败");
        }
    }

    @Operation(summary = "更新用户")
    @PutMapping("/{id}")
    public Result<Map<String, Object>> updateUser(@PathVariable Long id, @RequestBody Map<String, Object> userData) {
        try {
            String sql = "UPDATE tailor_is_user.sys_user SET real_name = ?, phone = ?, email = ?, status = ?, update_time = NOW() WHERE id = ?";
            jdbcTemplate.update(sql,
                    userData.get("realName"),
                    userData.get("phone"),
                    userData.get("email"),
                    userData.get("status"),
                    id);
            
            SysUser user = adminUserService.getUserDetail(id);
            if (user == null) {
                return Result.fail("用户不存在");
            }
            return Result.success(convertUserToMap(user));
        } catch (Exception e) {
            log.error("Failed to update user", e);
            return Result.fail("更新用户失败");
        }
    }

    @Operation(summary = "冻结用户")
    @PutMapping("/{id}/status")
    public Result<Void> toggleUserStatus(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        Integer status = body.get("status");
        if (status != null && status == 1) {
            adminUserService.freezeUser(id);
        } else {
            adminUserService.unfreezeUser(id);
        }
        return Result.success();
    }

    @Operation(summary = "重置密码")
    @PostMapping("/{id}/password/reset")
    public Result<String> resetPassword(@PathVariable Long id) {
        try {
            String newPassword = "admin123";
            String encodedPassword = passwordEncoder.encode(newPassword);
            jdbcTemplate.update("UPDATE tailor_is_user.sys_user SET password = ?, update_time = NOW() WHERE id = ?", encodedPassword, id);
            return Result.success(newPassword);
        } catch (Exception e) {
            log.error("Failed to reset password", e);
            return Result.fail("重置密码失败");
        }
    }

    @Operation(summary = "分配角色")
    @PutMapping("/{id}/roles")
    public Result<Void> assignRoles(@PathVariable Long id, @RequestBody Map<String, Long[]> body) {
        try {
            Long[] roleIds = body.get("roleIds");
            jdbcTemplate.update("DELETE FROM tailor_is_user.sys_user_role WHERE user_id = ?", id);
            if (roleIds != null && roleIds.length > 0) {
                StringBuilder sql = new StringBuilder("INSERT INTO tailor_is_user.sys_user_role (user_id, role_id) VALUES ");
                for (int i = 0; i < roleIds.length; i++) {
                    if (i > 0) {
                        sql.append(", ");
                    }
                    sql.append("(?, ?)");
                }
                Object[] params = new Object[roleIds.length * 2];
                for (int i = 0; i < roleIds.length; i++) {
                    params[i * 2] = id;
                    params[i * 2 + 1] = roleIds[i];
                }
                jdbcTemplate.update(sql.toString(), params);
            }
            return Result.success();
        } catch (Exception e) {
            log.error("Failed to assign roles", e);
            return Result.fail("分配角色失败");
        }
    }

    private Map<String, Object> convertUserToMap(SysUser user) {
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("id", user.getId());
        userMap.put("username", user.getUsername());
        userMap.put("realName", user.getRealName());
        userMap.put("phone", user.getPhone());
        userMap.put("email", user.getEmail());
        userMap.put("status", user.getStatus());
        userMap.put("createTime", user.getCreateTime());
        
        List<String> roles = jdbcTemplate.queryForList(
                "SELECT r.role_name FROM tailor_is_user.sys_role r JOIN tailor_is_user.sys_user_role ur ON r.id = ur.role_id WHERE ur.user_id = ?",
                String.class, user.getId());
        userMap.put("roles", roles);
        
        return userMap;
    }
}
