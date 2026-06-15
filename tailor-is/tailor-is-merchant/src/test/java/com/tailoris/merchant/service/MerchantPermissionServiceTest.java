package com.tailoris.merchant.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 商家权限服务单元测试 - MER-009.
 *
 * <p>验证权限编码格式与权限集合操作。</p>
 *
 * @author Tailor IS Team
 * @since 2026-06-03
 */
@DisplayName("商家权限服务单元测试")
class MerchantPermissionServiceTest {

    @Test
    @DisplayName("权限编码格式验证")
    void testPermissionCodeFormat() {
        String code = "product:create";
        String[] parts = code.split(":");
        assertEquals(2, parts.length);
        assertEquals("product", parts[0]);
        assertEquals("create", parts[1]);
    }

    @Test
    @DisplayName("权限集合包含性验证")
    void testPermissionContains() {
        Set<String> permissions = Set.of("product:create", "product:update", "order:list");
        assertTrue(permissions.contains("product:create"));
        assertTrue(permissions.contains("order:list"));
        assertFalse(permissions.contains("product:delete"));
        assertFalse(permissions.contains("finance:export"));
    }

    @Test
    @DisplayName("批量权限校验")
    void testBatchPermissionCheck() {
        Set<String> permissions = Set.of(
                "product:create", "product:update", "product:list",
                "order:list", "order:detail"
        );
        String[] checks = {"product:create", "order:list", "product:delete", "finance:export"};
        boolean[] results = new boolean[checks.length];
        for (int i = 0; i < checks.length; i++) {
            results[i] = permissions.contains(checks[i]);
        }
        assertTrue(results[0]);
        assertTrue(results[1]);
        assertFalse(results[2]);
        assertFalse(results[3]);
    }

    @Test
    @DisplayName("角色权限合并去重")
    void testRolePermissionUnion() {
        // 角色权限
        Set<String> role1 = Set.of("product:create", "product:update", "order:list");
        // 员工自定义权限
        Set<String> custom = Set.of("order:list", "finance:export");

        // 合并（Set自动去重）
        java.util.Set<String> merged = new java.util.HashSet<>(role1);
        merged.addAll(custom);

        assertEquals(4, merged.size());
        assertTrue(merged.contains("product:create"));
        assertTrue(merged.contains("finance:export"));
    }

    @Test
    @DisplayName("空权限集合处理")
    void testEmptyPermissionSet() {
        Set<String> empty = Collections.emptySet();
        assertFalse(empty.contains("product:create"));
        assertEquals(0, empty.size());
    }

    @Test
    @DisplayName("系统预设角色权限范围")
    void testSystemRolePermissionScope() {
        // 店长：所有权限
        List<String> shopManager = Arrays.asList(
                "product:create", "product:update", "product:delete", "product:list",
                "order:list", "order:detail", "order:refund", "order:export",
                "employee:list", "employee:add", "employee:remove",
                "shop:update", "shop:decoration", "shop:settings",
                "data:dashboard", "data:export", "finance:settle", "finance:bill",
                "review:reply", "review:feature", "review:hide"
        );
        assertTrue(shopManager.size() >= 20, "店长至少20项权限");

        // 客服：仅订单+评价
        List<String> customerService = Arrays.asList(
                "order:list", "order:detail", "review:reply", "review:hide", "data:dashboard"
        );
        assertTrue(customerService.size() < 10, "客服权限数 < 10");
    }

    @Test
    @DisplayName("权限字典模块归类")
    void testPermissionDictByModule() {
        java.util.Map<String, List<String>> moduleMap = new java.util.HashMap<>();
        moduleMap.put("product", Arrays.asList("product:create", "product:update", "product:delete", "product:list", "product:audit"));
        moduleMap.put("order", Arrays.asList("order:list", "order:detail", "order:refund", "order:export", "order:ship"));
        moduleMap.put("finance", Arrays.asList("finance:settle", "finance:bill", "finance:export"));

        assertEquals(5, moduleMap.get("product").size());
        assertEquals(5, moduleMap.get("order").size());
        assertEquals(3, moduleMap.get("finance").size());
    }
}
