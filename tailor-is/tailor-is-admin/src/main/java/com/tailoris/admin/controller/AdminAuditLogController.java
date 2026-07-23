package com.tailoris.admin.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.tailoris.common.constant.RoutePortConstant;
import com.tailoris.common.dto.PageResponse;
import com.tailoris.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Tag(name = "平台操作日志")
@RestController
@RequestMapping(RoutePortConstant.ROUTE_ADMIN_AUDIT_LOGS)
@RequiredArgsConstructor
@SaCheckRole({"SUPER_ADMIN", "PLATFORM_ADMIN"})
public class AdminAuditLogController {

    private final JdbcTemplate jdbcTemplate;

    @Operation(summary = "分页查询操作日志")
    @GetMapping
    public Result<PageResponse<Map<String, Object>>> listAuditLogs(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String actionType,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        try {
            int offset = (current - 1) * size;
            StringBuilder sql = new StringBuilder("SELECT id, operator, action_type AS actionType, target, description, ip, user_agent AS userAgent, request_params AS requestParams, create_time AS createTime FROM tailor_is_admin.audit_log WHERE deleted = 0");
            
            List<Object> params = new ArrayList<>();
            
            if (keyword != null && !keyword.isEmpty()) {
                sql.append(" AND (operator LIKE ? OR description LIKE ?)");
                params.add("%" + keyword + "%");
                params.add("%" + keyword + "%");
            }
            if (actionType != null && !actionType.isEmpty()) {
                sql.append(" AND action_type = ?");
                params.add(actionType);
            }
            if (startDate != null && !startDate.isEmpty()) {
                sql.append(" AND create_time >= ?");
                params.add(startDate + " 00:00:00");
            }
            if (endDate != null && !endDate.isEmpty()) {
                sql.append(" AND create_time <= ?");
                params.add(endDate + " 23:59:59");
            }
            
            sql.append(" ORDER BY create_time DESC LIMIT ?, ?");
            params.add(offset);
            params.add(size);
            
            List<Map<String, Object>> records = jdbcTemplate.queryForList(sql.toString(), params.toArray());
            
            StringBuilder countSql = new StringBuilder("SELECT COUNT(*) FROM tailor_is_admin.audit_log WHERE deleted = 0");
            if (keyword != null && !keyword.isEmpty()) {
                countSql.append(" AND (operator LIKE ? OR description LIKE ?)");
            }
            if (actionType != null && !actionType.isEmpty()) {
                countSql.append(" AND action_type = ?");
            }
            if (startDate != null && !startDate.isEmpty()) {
                countSql.append(" AND create_time >= ?");
            }
            if (endDate != null && !endDate.isEmpty()) {
                countSql.append(" AND create_time <= ?");
            }
            
            Long total = jdbcTemplate.queryForObject(countSql.toString(), Long.class, params.subList(0, params.size() - 2).toArray());
            
            return Result.success(new PageResponse<>(records, total, current, size));
        } catch (Exception e) {
            log.warn("Failed to query audit logs", e);
            return Result.success(new PageResponse<>(new ArrayList<>(), 0L, current, size));
        }
    }

    @Operation(summary = "获取操作日志详情")
    @GetMapping("/{id}")
    public Result<Map<String, Object>> getAuditLogDetail(@PathVariable Long id) {
        try {
            String sql = "SELECT id, operator, action_type AS actionType, target, description, ip, user_agent AS userAgent, request_params AS requestParams, create_time AS createTime FROM tailor_is_admin.audit_log WHERE id = ? AND deleted = 0";
            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, id);
            if (results.isEmpty()) {
                return Result.fail("日志不存在");
            }
            return Result.success(results.get(0));
        } catch (Exception e) {
            log.warn("Failed to get audit log detail", e);
            return Result.fail("日志不存在");
        }
    }
}