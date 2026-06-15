package com.tailoris.merchant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tailoris.merchant.entity.MerchantRoleTemplate;
import com.tailoris.merchant.exception.MerchantBusinessException;
import com.tailoris.merchant.mapper.MerchantRoleTemplateMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("商家角色模板服务测试")
class MerchantRoleTemplateServiceImplTest {

    @Mock
    private MerchantRoleTemplateMapper roleTemplateMapper;

    @InjectMocks
    private MerchantRoleTemplateServiceImpl roleTemplateService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(roleTemplateService, "baseMapper", roleTemplateMapper);
    }

    @Test
    @DisplayName("列出系统角色")
    void testListSystemRoles() {
        MerchantRoleTemplate role = new MerchantRoleTemplate();
        role.setRoleCode("shop_manager");
        role.setRoleType(1);

        when(roleTemplateMapper.selectSystemRoles()).thenReturn(Arrays.asList(role));

        List<MerchantRoleTemplate> result = roleTemplateService.listSystemRoles();

        assertEquals(1, result.size());
        assertEquals("shop_manager", result.get(0).getRoleCode());
    }

    @Test
    @DisplayName("列出商家角色")
    void testListMerchantRoles() {
        MerchantRoleTemplate role = new MerchantRoleTemplate();
        role.setRoleCode("custom_role");
        role.setMerchantId(10L);

        when(roleTemplateMapper.selectByMerchantId(10L)).thenReturn(Arrays.asList(role));

        List<MerchantRoleTemplate> result = roleTemplateService.listMerchantRoles(10L);

        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("获取角色权限：角色不存在")
    void testGetRoleWithPermissions_NotFound() {
        when(roleTemplateMapper.selectById(999L)).thenReturn(null);

        MerchantRoleTemplate result = roleTemplateService.getRoleWithPermissions(999L);

        assertNull(result);
    }

    @Test
    @DisplayName("获取角色权限：成功解析权限列表")
    void testGetRoleWithPermissions_Success() {
        MerchantRoleTemplate role = new MerchantRoleTemplate();
        role.setId(1L);
        role.setPermissions("[\"product:create\",\"order:read\"]");

        when(roleTemplateMapper.selectById(1L)).thenReturn(role);

        MerchantRoleTemplate result = roleTemplateService.getRoleWithPermissions(1L);

        assertNotNull(result);
        assertNotNull(result.getPermissionList());
        assertEquals(2, result.getPermissionList().size());
        assertTrue(result.getPermissionList().contains("product:create"));
    }

    @Test
    @DisplayName("根据角色代码获取角色")
    void testGetByRoleCode() {
        MerchantRoleTemplate role = new MerchantRoleTemplate();
        role.setRoleCode("shop_manager");
        role.setPermissions("[\"product:read\"]");

        when(roleTemplateMapper.selectByRoleCode("shop_manager")).thenReturn(role);

        MerchantRoleTemplate result = roleTemplateService.getByRoleCode("shop_manager");

        assertNotNull(result);
        assertNotNull(result.getPermissionList());
    }

    @Test
    @DisplayName("创建商家角色：成功")
    void testCreateMerchantRole_Success() {
        when(roleTemplateMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(roleTemplateMapper.insert(any(MerchantRoleTemplate.class))).thenReturn(1);

        MerchantRoleTemplate result = roleTemplateService.createMerchantRole(
                10L, "自定义角色", Arrays.asList("product:create", "order:read"), "测试角色");

        assertNotNull(result);
        assertEquals(10L, result.getMerchantId());
        assertEquals("自定义角色", result.getRoleName());
        assertEquals(2, result.getRoleType());
        assertEquals(1, result.getIsEnabled());
        assertNotNull(result.getPermissionList());
    }

    @Test
    @DisplayName("创建商家角色：角色名已存在")
    void testCreateMerchantRole_DuplicateName() {
        when(roleTemplateMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        assertThrows(MerchantBusinessException.class, () ->
                roleTemplateService.createMerchantRole(10L, "已有角色", null, "描述"));
    }

    @Test
    @DisplayName("更新权限：成功")
    void testUpdatePermissions_Success() {
        MerchantRoleTemplate role = new MerchantRoleTemplate();
        role.setId(1L);
        role.setRoleType(2);
        role.setPermissions("[]");

        when(roleTemplateMapper.selectById(1L)).thenReturn(role);
        when(roleTemplateMapper.updateById(any(MerchantRoleTemplate.class))).thenReturn(1);

        boolean result = roleTemplateService.updatePermissions(1L, Arrays.asList("product:create"));

        assertTrue(result);
    }

    @Test
    @DisplayName("更新权限：角色不存在")
    void testUpdatePermissions_RoleNotFound() {
        when(roleTemplateMapper.selectById(999L)).thenReturn(null);

        assertThrows(MerchantBusinessException.class, () ->
                roleTemplateService.updatePermissions(999L, Arrays.asList("product:create")));
    }

    @Test
    @DisplayName("更新权限：系统预设角色不可修改")
    void testUpdatePermissions_SystemRole() {
        MerchantRoleTemplate role = new MerchantRoleTemplate();
        role.setId(1L);
        role.setRoleType(1);

        when(roleTemplateMapper.selectById(1L)).thenReturn(role);

        assertThrows(MerchantBusinessException.class, () ->
                roleTemplateService.updatePermissions(1L, Arrays.asList("product:create")));
    }

    @Test
    @DisplayName("删除角色：成功")
    void testDeleteRole_Success() {
        MerchantRoleTemplate role = new MerchantRoleTemplate();
        role.setId(1L);
        role.setRoleType(2);
        role.setMerchantId(10L);

        when(roleTemplateMapper.selectById(1L)).thenReturn(role);
        when(roleTemplateMapper.deleteById(1L)).thenReturn(1);

        boolean result = roleTemplateService.deleteRole(1L, 10L);

        assertTrue(result);
    }

    @Test
    @DisplayName("删除角色：角色不存在")
    void testDeleteRole_NotFound() {
        when(roleTemplateMapper.selectById(999L)).thenReturn(null);

        assertThrows(MerchantBusinessException.class, () ->
                roleTemplateService.deleteRole(999L, 10L));
    }

    @Test
    @DisplayName("删除角色：系统预设角色不可删除")
    void testDeleteRole_SystemRole() {
        MerchantRoleTemplate role = new MerchantRoleTemplate();
        role.setId(1L);
        role.setRoleType(1);

        when(roleTemplateMapper.selectById(1L)).thenReturn(role);

        assertThrows(MerchantBusinessException.class, () ->
                roleTemplateService.deleteRole(1L, 10L));
    }

    @Test
    @DisplayName("删除角色：无权删除")
    void testDeleteRole_NoPermission() {
        MerchantRoleTemplate role = new MerchantRoleTemplate();
        role.setId(1L);
        role.setRoleType(2);
        role.setMerchantId(20L);

        when(roleTemplateMapper.selectById(1L)).thenReturn(role);

        assertThrows(MerchantBusinessException.class, () ->
                roleTemplateService.deleteRole(1L, 10L));
    }
}
