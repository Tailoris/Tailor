package com.tailoris.admin.config;

import cn.dev33.satoken.stp.StpInterface;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class SaTokenStpInterfaceImpl implements StpInterface {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        try {
            Long userId = Long.parseLong(String.valueOf(loginId));
            String sql = """
                SELECT p.permission_code 
                FROM tailor_is_user.sys_role_permission rp
                JOIN tailor_is_user.sys_permission p ON rp.permission_id = p.id
                JOIN tailor_is_user.sys_user_role ur ON rp.role_id = ur.role_id
                WHERE ur.user_id = ? AND p.status = 1 AND p.deleted = 0
            """;
            List<Map<String, Object>> result = jdbcTemplate.queryForList(sql, userId);
            List<String> permissions = new ArrayList<>();
            for (Map<String, Object> row : result) {
                permissions.add((String) row.get("permission_code"));
            }
            return permissions;
        } catch (Exception e) {
            log.warn("获取用户权限失败: loginId={}, error={}", loginId, e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        try {
            Long userId = Long.parseLong(String.valueOf(loginId));
            String sql = """
                SELECT r.role_code 
                FROM tailor_is_user.sys_user_role ur
                JOIN tailor_is_user.sys_role r ON ur.role_id = r.id
                WHERE ur.user_id = ? AND r.status = 1 AND r.deleted = 0
            """;
            List<Map<String, Object>> result = jdbcTemplate.queryForList(sql, userId);
            List<String> roles = new ArrayList<>();
            for (Map<String, Object> row : result) {
                roles.add((String) row.get("role_code"));
            }
            log.debug("获取用户角色成功: loginId={}, roles={}", loginId, roles);
            return roles;
        } catch (Exception e) {
            log.warn("获取用户角色失败: loginId={}, error={}", loginId, e.getMessage());
            return new ArrayList<>();
        }
    }
}
