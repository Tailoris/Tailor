package com.tailoris.admin.service.impl;

import com.tailoris.admin.service.AdminRoleService;
import com.tailoris.common.dto.PageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminRoleServiceImpl implements AdminRoleService {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public PageResponse<Map<String, Object>> listRoles(String keyword, Integer current, Integer size) {
        int offset = (current - 1) * size;
        StringBuilder countSql = new StringBuilder("SELECT COUNT(*) FROM tailor_is_user.sys_role WHERE deleted = 0");
        StringBuilder sql = new StringBuilder("SELECT id, role_name AS name, role_code AS code, description, create_time FROM tailor_is_user.sys_role WHERE deleted = 0");

        if (keyword != null && !keyword.isEmpty()) {
            countSql.append(" AND (role_name LIKE ? OR role_code LIKE ?)");
            sql.append(" AND (role_name LIKE ? OR role_code LIKE ?)");
        }
        sql.append(" ORDER BY sort ASC, create_time DESC LIMIT ?, ?");

        Long total;
        if (keyword != null && !keyword.isEmpty()) {
            String likeKeyword = "%" + keyword + "%";
            total = jdbcTemplate.queryForObject(countSql.toString(), Long.class, likeKeyword, likeKeyword);
        } else {
            total = jdbcTemplate.queryForObject(countSql.toString(), Long.class);
        }

        List<Map<String, Object>> records;
        if (keyword != null && !keyword.isEmpty()) {
            String likeKeyword = "%" + keyword + "%";
            records = jdbcTemplate.queryForList(sql.toString(), likeKeyword, likeKeyword, offset, size);
        } else {
            records = jdbcTemplate.queryForList(sql.toString(), offset, size);
        }

        for (Map<String, Object> record : records) {
            Long roleId = ((Number) record.get("id")).longValue();
            Integer userCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM tailor_is_user.sys_user_role WHERE role_id = ?",
                    Integer.class, roleId);
            record.put("userCount", userCount);
        }

        return new PageResponse<>(records, total, current, size);
    }

    @Override
    public Map<String, Object> getRoleDetail(Long id) {
        String sql = "SELECT id, role_name AS name, role_code AS code, description, create_time FROM tailor_is_user.sys_role WHERE id = ? AND deleted = 0";
        List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, id);
        if (results.isEmpty()) {
            return null;
        }
        Map<String, Object> role = results.get(0);
        Integer userCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tailor_is_user.sys_user_role WHERE role_id = ?",
                Integer.class, id);
        role.put("userCount", userCount);
        return role;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> createRole(Map<String, Object> role) {
        String sql = "INSERT INTO tailor_is_user.sys_role (role_name, role_code, description, data_scope, sort, status, create_time, update_time) " +
                "VALUES (?, ?, ?, 1, 0, 1, NOW(), NOW())";
        jdbcTemplate.update(sql,
                role.get("name"),
                role.get("code"),
                role.get("description"));

        Long id = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        return getRoleDetail(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> updateRole(Long id, Map<String, Object> role) {
        String sql = "UPDATE tailor_is_user.sys_role SET role_name = ?, role_code = ?, description = ?, update_time = NOW() WHERE id = ?";
        jdbcTemplate.update(sql,
                role.get("name"),
                role.get("code"),
                role.get("description"),
                id);
        return getRoleDetail(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRole(Long id) {
        jdbcTemplate.update("UPDATE tailor_is_user.sys_role SET deleted = 1 WHERE id = ?", id);
        jdbcTemplate.update("DELETE FROM tailor_is_user.sys_user_role WHERE role_id = ?", id);
        jdbcTemplate.update("DELETE FROM tailor_is_user.sys_role_permission WHERE role_id = ?", id);
    }

    @Override
    public List<Map<String, Object>> getRolePermissions(Long roleId) {
        String sql = "SELECT p.id, p.permission_name AS name, p.permission_code AS code, p.type, p.parent_id AS parentId " +
                "FROM tailor_is_user.sys_permission p " +
                "LEFT JOIN tailor_is_user.sys_role_permission rp ON p.id = rp.permission_id AND rp.role_id = ? " +
                "WHERE p.deleted = 0 AND p.status = 1 " +
                "ORDER BY p.sort ASC";
        return jdbcTemplate.queryForList(sql, roleId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignRolePermissions(Long roleId, Long[] permissionIds) {
        jdbcTemplate.update("DELETE FROM tailor_is_user.sys_role_permission WHERE role_id = ?", roleId);
        if (permissionIds != null && permissionIds.length > 0) {
            StringBuilder sql = new StringBuilder("INSERT INTO tailor_is_user.sys_role_permission (role_id, permission_id) VALUES ");
            for (int i = 0; i < permissionIds.length; i++) {
                if (i > 0) {
                    sql.append(", ");
                }
                sql.append("(?, ?)");
            }
            Object[] params = new Object[permissionIds.length * 2];
            for (int i = 0; i < permissionIds.length; i++) {
                params[i * 2] = roleId;
                params[i * 2 + 1] = permissionIds[i];
            }
            jdbcTemplate.update(sql.toString(), params);
        }
    }

    @Override
    public PageResponse<Map<String, Object>> getRoleUsers(Long roleId, Integer current, Integer size) {
        int offset = (current - 1) * size;
        String countSql = "SELECT COUNT(*) FROM tailor_is_user.sys_user u JOIN tailor_is_user.sys_user_role ur ON u.id = ur.user_id WHERE ur.role_id = ? AND u.deleted = 0";
        String sql = "SELECT u.id, u.username, u.nick_name AS nickname FROM tailor_is_user.sys_user u " +
                "JOIN tailor_is_user.sys_user_role ur ON u.id = ur.user_id " +
                "WHERE ur.role_id = ? AND u.deleted = 0 ORDER BY u.create_time DESC LIMIT ?, ?";

        Long total = jdbcTemplate.queryForObject(countSql, Long.class, roleId);
        List<Map<String, Object>> records = jdbcTemplate.queryForList(sql, roleId, offset, size);

        return new PageResponse<>(records, total, current, size);
    }

    @Override
    public List<Map<String, Object>> listAllRoles() {
        String sql = "SELECT id, role_name AS name, role_code AS code FROM tailor_is_user.sys_role WHERE deleted = 0 AND status = 1 ORDER BY sort ASC";
        return jdbcTemplate.queryForList(sql);
    }
}