package com.tailoris.merchant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tailoris.merchant.entity.MerchantRole;
import com.tailoris.merchant.mapper.MerchantRoleMapper;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("商家角色服务单元测试")
@ExtendWith(MockitoExtension.class)
class MerchantRoleServiceImplTest {

    @Mock
    private MerchantRoleMapper merchantRoleMapper;

    @InjectMocks
    private MerchantRoleServiceImpl merchantRoleService;

    private MerchantRole role;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(merchantRoleService, "baseMapper", merchantRoleMapper);
        role = new MerchantRole();
        role.setId(1L);
        role.setMerchantId(100L);
        role.setRoleName("运营");
        role.setRoleCode("operator");
        role.setDescription("负责商品运营");
        role.setStatus(1);
        role.setSortOrder(1);
        role.setSystemRole(false);
        role.setDeleted(0);
    }

    @Test
    @DisplayName("创建角色：merchantId为空应抛异常")
    void testCreateRole_MerchantIdNull() {
        role.setMerchantId(null);

        assertThrows(IllegalArgumentException.class, () -> merchantRoleService.createRole(role));
    }

    @Test
    @DisplayName("创建角色：超过最大数量应抛异常")
    void testCreateRole_ExceedMaxCount() {
        when(merchantRoleMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(20L);

        assertThrows(IllegalStateException.class, () -> merchantRoleService.createRole(role));
    }

    @Test
    @DisplayName("创建角色：角色编码已存在应抛异常")
    void testCreateRole_RoleCodeExists() {
        when(merchantRoleMapper.selectCount(any(LambdaQueryWrapper.class)))
                .thenReturn(0L)
                .thenReturn(1L);

        assertThrows(IllegalArgumentException.class, () -> merchantRoleService.createRole(role));
    }

    @Test
    @DisplayName("创建角色：成功创建")
    void testCreateRole_Success() {
        when(merchantRoleMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(merchantRoleMapper.insert(any(MerchantRole.class))).thenReturn(1);

        MerchantRole result = merchantRoleService.createRole(role);

        assertNotNull(result);
        assertEquals("运营", result.getRoleName());
        assertEquals(1, result.getStatus());
        verify(merchantRoleMapper).insert(any(MerchantRole.class));
    }

    @Test
    @DisplayName("更新角色：角色ID为空应抛异常")
    void testUpdateRole_RoleIdNull() {
        role.setId(null);

        assertThrows(IllegalArgumentException.class, () -> merchantRoleService.updateRole(role));
    }

    @Test
    @DisplayName("更新角色：角色不存在应抛异常")
    void testUpdateRole_NotFound() {
        role.setId(999L);
        when(merchantRoleMapper.selectById(999L)).thenReturn(null);

        assertThrows(IllegalArgumentException.class, () -> merchantRoleService.updateRole(role));
    }

    @Test
    @DisplayName("更新角色：系统角色不可修改权限")
    void testUpdateRole_SystemRoleCannotModifyPermissions() {
        role.setSystemRole(true);
        role.setPermissions("new:permission");
        when(merchantRoleMapper.selectById(1L)).thenReturn(role);

        assertThrows(IllegalStateException.class, () -> merchantRoleService.updateRole(role));
    }

    @Test
    @DisplayName("删除角色：角色不存在返回false")
    void testDeleteRole_NotFound() {
        when(merchantRoleMapper.selectById(999L)).thenReturn(null);

        boolean result = merchantRoleService.deleteRole(999L, 100L);

        assertFalse(result);
    }

    @Test
    @DisplayName("删除角色：无权操作应抛异常")
    void testDeleteRole_Unauthorized() {
        role.setMerchantId(200L);
        when(merchantRoleMapper.selectById(1L)).thenReturn(role);

        assertThrows(SecurityException.class, () -> merchantRoleService.deleteRole(1L, 100L));
    }

    @Test
    @DisplayName("删除角色：系统角色不可删除")
    void testDeleteRole_SystemRoleCannotDelete() {
        role.setSystemRole(true);
        when(merchantRoleMapper.selectById(1L)).thenReturn(role);

        assertThrows(IllegalStateException.class, () -> merchantRoleService.deleteRole(1L, 100L));
    }

    @Test
    @DisplayName("删除角色：成功删除")
    void testDeleteRole_Success() {
        when(merchantRoleMapper.selectById(1L)).thenReturn(role);
        when(merchantRoleMapper.deleteById(1L)).thenReturn(1);

        boolean result = merchantRoleService.deleteRole(1L, 100L);

        assertTrue(result);
        verify(merchantRoleMapper).deleteById(1L);
    }

    @Test
    @DisplayName("列出商家角色：成功返回")
    void testListByMerchant_Success() {
        when(merchantRoleMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Arrays.asList(role));

        List<MerchantRole> result = merchantRoleService.listByMerchant(100L);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("获取角色详情：成功返回")
    void testGetRoleWithPermissions_Success() {
        when(merchantRoleMapper.selectById(1L)).thenReturn(role);

        MerchantRole result = merchantRoleService.getRoleWithPermissions(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
    }
}
