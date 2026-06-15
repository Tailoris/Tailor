package com.tailoris.merchant.service.impl;

import com.tailoris.merchant.entity.MerchantEmployee;
import com.tailoris.merchant.entity.MerchantRoleTemplate;
import com.tailoris.merchant.mapper.MerchantEmployeeMapper;
import com.tailoris.merchant.mapper.MerchantRoleTemplateMapper;
import com.tailoris.merchant.service.IMerchantRoleTemplateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("商家权限服务测试")
class MerchantPermissionServiceImplTest {

    @Mock
    private MerchantEmployeeMapper employeeMapper;

    @Mock
    private MerchantRoleTemplateMapper roleTemplateMapper;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ObjectProvider<IMerchantRoleTemplateService> roleTemplateServiceProvider;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private MerchantPermissionServiceImpl permissionService;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    @DisplayName("检查权限：employeeId为null")
    void testHasPermission_NullEmployeeId() {
        boolean result = permissionService.hasPermission(null, "product:create");
        assertFalse(result);
    }

    @Test
    @DisplayName("检查权限：permissionCode为null")
    void testHasPermission_NullPermissionCode() {
        boolean result = permissionService.hasPermission(1L, null);
        assertFalse(result);
    }

    @Test
    @DisplayName("检查权限：有权限（缓存命中）")
    void testHasPermission_CacheHit() {
        when(valueOperations.get("merchant:emp:perm:1")).thenReturn("product:create,product:read");

        boolean result = permissionService.hasPermission(1L, "product:create");

        assertTrue(result);
    }

    @Test
    @DisplayName("检查权限：无权限")
    void testHasPermission_NoPermission() {
        when(valueOperations.get("merchant:emp:perm:1")).thenReturn("product:read");

        boolean result = permissionService.hasPermission(1L, "product:create");

        assertFalse(result);
    }

    @Test
    @DisplayName("获取员工权限：缓存未命中从DB加载")
    void testGetEmployeePermissions_FromDb() {
        when(valueOperations.get("merchant:emp:perm:1")).thenReturn(null);

        MerchantEmployee employee = new MerchantEmployee();
        employee.setId(1L);
        employee.setRoleCode("shop_manager");
        employee.setPermissions("[\"product:create\"]");

        MerchantRoleTemplate role = new MerchantRoleTemplate();
        role.setPermissions("[\"product:read\",\"order:read\"]");

        when(employeeMapper.selectById(1L)).thenReturn(employee);
        when(roleTemplateMapper.selectByRoleCode("shop_manager")).thenReturn(role);

        Set<String> permissions = permissionService.getEmployeePermissions(1L);

        assertTrue(permissions.contains("product:create"));
        assertTrue(permissions.contains("product:read"));
        assertTrue(permissions.contains("order:read"));
        verify(valueOperations).set(eq("merchant:emp:perm:1"), anyString(), any());
    }

    @Test
    @DisplayName("获取员工权限：员工不存在")
    void testGetEmployeePermissions_EmployeeNotFound() {
        when(valueOperations.get("merchant:emp:perm:1")).thenReturn(null);
        when(employeeMapper.selectById(1L)).thenReturn(null);

        Set<String> permissions = permissionService.getEmployeePermissions(1L);

        assertTrue(permissions.isEmpty());
    }

    @Test
    @DisplayName("获取员工权限：Redis读取异常")
    void testGetEmployeePermissions_RedisReadError() {
        when(valueOperations.get("merchant:emp:perm:1")).thenThrow(new RuntimeException("Redis error"));

        MerchantEmployee employee = new MerchantEmployee();
        employee.setId(1L);
        employee.setRoleCode("staff");
        employee.setPermissions(null);

        MerchantRoleTemplate role = new MerchantRoleTemplate();
        role.setPermissions("[\"product:read\"]");

        when(employeeMapper.selectById(1L)).thenReturn(employee);
        when(roleTemplateMapper.selectByRoleCode("staff")).thenReturn(role);

        Set<String> permissions = permissionService.getEmployeePermissions(1L);

        assertTrue(permissions.contains("product:read"));
    }

    @Test
    @DisplayName("批量检查权限：空数组")
    void testHasPermissions_EmptyArray() {
        boolean[] result = permissionService.hasPermissions(1L, new String[]{});
        assertEquals(0, result.length);
    }

    @Test
    @DisplayName("批量检查权限：null数组")
    void testHasPermissions_NullArray() {
        boolean[] result = permissionService.hasPermissions(1L, null);
        assertEquals(0, result.length);
    }

    @Test
    @DisplayName("批量检查权限：混合结果")
    void testHasPermissions_MixedResults() {
        when(valueOperations.get("merchant:emp:perm:1")).thenReturn("product:create,order:read");

        boolean[] result = permissionService.hasPermissions(1L,
                new String[]{"product:create", "product:delete", "order:read"});

        assertEquals(3, result.length);
        assertTrue(result[0]);
        assertFalse(result[1]);
        assertTrue(result[2]);
    }

    @Test
    @DisplayName("获取角色默认权限：角色不存在")
    void testGetDefaultPermissionsByRoleCode_RoleNotFound() {
        when(roleTemplateMapper.selectByRoleCode("unknown")).thenReturn(null);

        List<String> result = permissionService.getDefaultPermissionsByRoleCode("unknown");

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("获取角色默认权限：roleCode为null")
    void testGetDefaultPermissionsByRoleCode_NullRoleCode() {
        List<String> result = permissionService.getDefaultPermissionsByRoleCode(null);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("获取角色默认权限：成功")
    void testGetDefaultPermissionsByRoleCode_Success() {
        MerchantRoleTemplate role = new MerchantRoleTemplate();
        role.setPermissions("[\"product:create\",\"product:read\"]");

        when(roleTemplateMapper.selectByRoleCode("shop_manager")).thenReturn(role);

        List<String> result = permissionService.getDefaultPermissionsByRoleCode("shop_manager");

        assertEquals(2, result.size());
        assertTrue(result.contains("product:create"));
        assertTrue(result.contains("product:read"));
    }

    @Test
    @DisplayName("检查店铺访问权限：员工不存在")
    void testCanAccessShop_EmployeeNotFound() {
        when(employeeMapper.selectById(1L)).thenReturn(null);

        boolean result = permissionService.canAccessShop(1L, 100L);

        assertFalse(result);
    }

    @Test
    @DisplayName("检查店铺访问权限：店长可访问全部")
    void testCanAccessShop_ShopManager() {
        MerchantEmployee employee = new MerchantEmployee();
        employee.setId(1L);
        employee.setRoleCode("shop_manager");

        when(employeeMapper.selectById(1L)).thenReturn(employee);

        assertTrue(permissionService.canAccessShop(1L, 100L));
    }

    @Test
    @DisplayName("检查店铺访问权限：商家所有者可访问全部")
    void testCanAccessShop_MerchantOwner() {
        MerchantEmployee employee = new MerchantEmployee();
        employee.setId(1L);
        employee.setRoleCode("merchant_owner");

        when(employeeMapper.selectById(1L)).thenReturn(employee);

        assertTrue(permissionService.canAccessShop(1L, 100L));
    }

    @Test
    @DisplayName("检查店铺访问权限：主店铺")
    void testCanAccessShop_PrimaryShop() {
        MerchantEmployee employee = new MerchantEmployee();
        employee.setId(1L);
        employee.setRoleCode("staff");
        employee.setShopId(100L);

        when(employeeMapper.selectById(1L)).thenReturn(employee);

        assertTrue(permissionService.canAccessShop(1L, 100L));
    }

    @Test
    @DisplayName("检查店铺访问权限：限定店铺列表内")
    void testCanAccessShop_InAllowedList() {
        MerchantEmployee employee = new MerchantEmployee();
        employee.setId(1L);
        employee.setRoleCode("staff");
        employee.setShopId(50L);
        employee.setShopIds("100,200,300");

        when(employeeMapper.selectById(1L)).thenReturn(employee);

        assertTrue(permissionService.canAccessShop(1L, 200L));
    }

    @Test
    @DisplayName("检查店铺访问权限：限定店铺列表外")
    void testCanAccessShop_NotInAllowedList() {
        MerchantEmployee employee = new MerchantEmployee();
        employee.setId(1L);
        employee.setRoleCode("staff");
        employee.setShopId(50L);
        employee.setShopIds("100,200");

        when(employeeMapper.selectById(1L)).thenReturn(employee);

        assertFalse(permissionService.canAccessShop(1L, 300L));
    }

    @Test
    @DisplayName("检查店铺访问权限：未限定店铺")
    void testCanAccessShop_NoRestriction() {
        MerchantEmployee employee = new MerchantEmployee();
        employee.setId(1L);
        employee.setRoleCode("staff");
        employee.setShopId(50L);
        employee.setShopIds(null);

        when(employeeMapper.selectById(1L)).thenReturn(employee);

        assertTrue(permissionService.canAccessShop(1L, 999L));
    }

    @Test
    @DisplayName("刷新员工权限缓存：成功")
    void testRefreshEmployeePermissions_Success() {
        when(redisTemplate.delete("merchant:emp:perm:1")).thenReturn(true);

        permissionService.refreshEmployeePermissions(1L);

        verify(redisTemplate).delete("merchant:emp:perm:1");
    }

    @Test
    @DisplayName("刷新员工权限缓存：Redis异常")
    void testRefreshEmployeePermissions_RedisError() {
        doThrow(new RuntimeException("Redis error")).when(redisTemplate).delete("merchant:emp:perm:1");

        assertDoesNotThrow(() -> permissionService.refreshEmployeePermissions(1L));
    }
}
