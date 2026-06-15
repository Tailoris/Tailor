package com.tailoris.merchant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tailoris.merchant.entity.MerchantEmployee;
import com.tailoris.merchant.entity.MerchantRoleTemplate;
import com.tailoris.merchant.mapper.MerchantEmployeeMapper;
import com.tailoris.merchant.mapper.MerchantRoleTemplateMapper;
import com.tailoris.merchant.service.IMerchantPermissionService;
import com.tailoris.merchant.service.IMerchantRoleTemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 商家权限服务实现（按钮级） - MER-002.
 *
 * <h3>权限解析优先级</h3>
 * <ol>
 *   <li>员工自定义权限（merchant_employee.permissions）</li>
 *   <li>员工角色对应权限（merchant_role_template.permissions）</li>
 *   <li>系统默认（按 role 字段）</li>
 * </ol>
 *
 * <h3>缓存策略</h3>
 * <p>员工权限在 Redis 中缓存5分钟，避免每次查询数据库</p>
 *
 * @author Tailor IS Team
 * @since 2026-06-03
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MerchantPermissionServiceImpl implements IMerchantPermissionService {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final long CACHE_MINUTES = 5L;

    private final MerchantEmployeeMapper employeeMapper;
    private final MerchantRoleTemplateMapper roleTemplateMapper;
    private final StringRedisTemplate redisTemplate;

    private final ObjectProvider<IMerchantRoleTemplateService> roleTemplateServiceProvider;

    @Override
    public boolean hasPermission(Long employeeId, String permissionCode) {
        if (employeeId == null || permissionCode == null) {
            return false;
        }
        Set<String> permissions = getEmployeePermissions(employeeId);
        return permissions.contains(permissionCode);
    }

    @Override
    public Set<String> getEmployeePermissions(Long employeeId) {
        String cacheKey = "merchant:emp:perm:" + employeeId;
        try {
            String cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null && !cached.isEmpty()) {
                return new HashSet<>(Arrays.asList(cached.split(",")));
            }
        } catch (Exception e) {
            log.debug("Redis读取权限缓存失败: {}", e.getMessage());
        }

        Set<String> permissions = loadPermissionsFromDb(employeeId);

        try {
            redisTemplate.opsForValue().set(cacheKey, String.join(",", permissions),
                    Duration.ofMinutes(CACHE_MINUTES));
        } catch (Exception e) {
            log.debug("Redis写入权限缓存失败: {}", e.getMessage());
        }
        return permissions;
    }

    @Override
    public boolean[] hasPermissions(Long employeeId, String[] permissionCodes) {
        if (permissionCodes == null || permissionCodes.length == 0) {
            return new boolean[0];
        }
        Set<String> permissions = getEmployeePermissions(employeeId);
        boolean[] result = new boolean[permissionCodes.length];
        for (int i = 0; i < permissionCodes.length; i++) {
            result[i] = permissions.contains(permissionCodes[i]);
        }
        return result;
    }

    @Override
    public List<String> getDefaultPermissionsByRoleCode(String roleCode) {
        if (roleCode == null) {
            return Collections.emptyList();
        }
        MerchantRoleTemplate role = roleTemplateMapper.selectByRoleCode(roleCode);
        if (role == null) {
            return Collections.emptyList();
        }
        return parseJson(role.getPermissions());
    }

    @Override
    public boolean canAccessShop(Long employeeId, Long shopId) {
        MerchantEmployee employee = employeeMapper.selectById(employeeId);
        if (employee == null) {
            return false;
        }
        // 店长 / 商家所有者 全部店铺
        if ("shop_manager".equals(employee.getRoleCode())
                || "merchant_owner".equals(employee.getRoleCode())) {
            return true;
        }
        // 主店铺
        if (shopId.equals(employee.getShopId())) {
            return true;
        }
        // 限定店铺列表
        if (employee.getShopIds() != null && !employee.getShopIds().isEmpty()) {
            return Arrays.asList(employee.getShopIds().split(",")).contains(shopId.toString());
        }
        return true;  // 未限定
    }

    @Override
    public void refreshEmployeePermissions(Long employeeId) {
        String cacheKey = "merchant:emp:perm:" + employeeId;
        try {
            redisTemplate.delete(cacheKey);
        } catch (Exception e) {
            log.warn("清除员工权限缓存失败: {}", e.getMessage());
        }
    }

    // ============================================================
    // 私有方法
    // ============================================================

    private Set<String> loadPermissionsFromDb(Long employeeId) {
        Set<String> permissions = new HashSet<>();
        MerchantEmployee employee = employeeMapper.selectById(employeeId);
        if (employee == null) {
            return permissions;
        }

        // 1. 角色权限
        if (employee.getRoleCode() != null && !employee.getRoleCode().isEmpty()) {
            MerchantRoleTemplate role = roleTemplateMapper.selectByRoleCode(employee.getRoleCode());
            if (role != null) {
                permissions.addAll(parseJson(role.getPermissions()));
            }
        }

        // 2. 员工自定义权限（覆盖）
        if (employee.getPermissions() != null && !employee.getPermissions().isEmpty()) {
            permissions.addAll(parseJson(employee.getPermissions()));
        }

        return permissions;
    }

    private List<String> parseJson(String json) {
        if (json == null || json.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            return MAPPER.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.warn("权限JSON解析失败: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}
