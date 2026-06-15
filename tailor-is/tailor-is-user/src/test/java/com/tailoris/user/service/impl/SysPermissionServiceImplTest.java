package com.tailoris.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tailoris.common.exception.BusinessException;
import com.tailoris.user.entity.SysPermission;
import com.tailoris.user.entity.SysRolePermission;
import com.tailoris.user.mapper.SysPermissionMapper;
import com.tailoris.user.mapper.SysRolePermissionMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("SysPermissionServiceImpl 单元测试")
@ExtendWith(MockitoExtension.class)
class SysPermissionServiceImplTest {

    @Mock
    private SysPermissionMapper sysPermissionMapper;

    @Mock
    private SysRolePermissionMapper sysRolePermissionMapper;

    @InjectMocks
    private SysPermissionServiceImpl sysPermissionService;

    private SysPermission mockPermission;
    private SysRolePermission mockRolePermission;

    @BeforeEach
    void setUp() {
        mockPermission = new SysPermission();
        mockPermission.setId(1L);
        mockPermission.setPermissionName("用户管理");
        mockPermission.setPermissionCode("user:manage");
        mockPermission.setType(1);
        mockPermission.setParentId(0L);
        mockPermission.setSort(1);
        mockPermission.setVisible(1);
        mockPermission.setStatus(1);

        mockRolePermission = new SysRolePermission();
        mockRolePermission.setId(1L);
        mockRolePermission.setRoleId(1L);
        mockRolePermission.setPermissionId(1L);
    }

    // ==================== getPermissionsByUserId ====================

    @Test
    @DisplayName("根据用户ID获取权限列表 - 有权限")
    void testGetPermissionsByUserId_HasPermissions() {
        List<SysPermission> permissions = List.of(mockPermission);
        when(sysPermissionMapper.selectByUserId(100L)).thenReturn(permissions);

        List<SysPermission> result = sysPermissionService.getPermissionsByUserId(100L);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("user:manage", result.get(0).getPermissionCode());
        verify(sysPermissionMapper).selectByUserId(100L);
    }

    @Test
    @DisplayName("根据用户ID获取权限列表 - 无权限")
    void testGetPermissionsByUserId_NoPermissions() {
        when(sysPermissionMapper.selectByUserId(200L)).thenReturn(Collections.emptyList());

        List<SysPermission> result = sysPermissionService.getPermissionsByUserId(200L);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ==================== getPermissionsByRoleId ====================

    @Test
    @DisplayName("根据角色ID获取权限列表 - 有权限")
    void testGetPermissionsByRoleId_HasPermissions() {
        SysPermission perm2 = new SysPermission();
        perm2.setId(2L);
        perm2.setPermissionCode("order:view");
        perm2.setStatus(1);
        perm2.setSort(2);

        SysRolePermission rp2 = new SysRolePermission();
        rp2.setPermissionId(2L);
        rp2.setRoleId(1L);

        when(sysRolePermissionMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(mockRolePermission, rp2));
        when(sysPermissionMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(mockPermission, perm2));

        List<SysPermission> result = sysPermissionService.getPermissionsByRoleId(1L);

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(sysRolePermissionMapper).selectList(any(LambdaQueryWrapper.class));
        verify(sysPermissionMapper).selectList(any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("根据角色ID获取权限列表 - 无权限返回空列表")
    void testGetPermissionsByRoleId_NoPermissions() {
        when(sysRolePermissionMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of());

        List<SysPermission> result = sysPermissionService.getPermissionsByRoleId(1L);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(sysPermissionMapper, never()).selectList(any());
    }

    // ==================== listAllPermissions ====================

    @Test
    @DisplayName("获取所有权限列表 - 成功")
    void testListAllPermissions_Success() {
        SysPermission perm2 = new SysPermission();
        perm2.setId(2L);
        perm2.setPermissionCode("product:manage");
        perm2.setSort(2);
        perm2.setStatus(1);

        when(sysPermissionMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(mockPermission, perm2));

        List<SysPermission> result = sysPermissionService.listAllPermissions();

        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    @DisplayName("获取所有权限列表 - 为空")
    void testListAllPermissions_Empty() {
        when(sysPermissionMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of());

        List<SysPermission> result = sysPermissionService.listAllPermissions();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ==================== getPermissionById ====================

    @Test
    @DisplayName("根据ID获取权限 - 成功")
    void testGetPermissionById_Success() {
        when(sysPermissionMapper.selectById(1L)).thenReturn(mockPermission);

        SysPermission result = sysPermissionService.getPermissionById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("user:manage", result.getPermissionCode());
    }

    @Test
    @DisplayName("根据ID获取权限 - 不存在抛异常")
    void testGetPermissionById_NotFound() {
        when(sysPermissionMapper.selectById(999L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> sysPermissionService.getPermissionById(999L));
        assertTrue(ex.getMessage().contains("权限不存在"));
    }

    // ==================== createPermission ====================

    @Test
    @DisplayName("创建权限 - 成功")
    void testCreatePermission_Success() {
        SysPermission newPerm = new SysPermission();
        newPerm.setPermissionName("订单管理");
        newPerm.setPermissionCode("order:manage");
        newPerm.setType(1);
        newPerm.setSort(10);

        when(sysPermissionMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(sysPermissionMapper.insert(any(SysPermission.class))).thenReturn(1);

        SysPermission result = sysPermissionService.createPermission(newPerm);

        assertNotNull(result);
        assertEquals("order:manage", result.getPermissionCode());
        assertEquals(1, result.getStatus());
        assertEquals(0L, result.getParentId()); // 默认 parentId
        assertEquals(1, result.getVisible()); // 默认 visible
        verify(sysPermissionMapper).selectCount(any(LambdaQueryWrapper.class));
        verify(sysPermissionMapper).insert(any(SysPermission.class));
    }

    @Test
    @DisplayName("创建权限 - 权限编码已存在抛异常")
    void testCreatePermission_CodeExists() {
        SysPermission newPerm = new SysPermission();
        newPerm.setPermissionCode("user:manage");

        when(sysPermissionMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> sysPermissionService.createPermission(newPerm));
        assertTrue(ex.getMessage().contains("权限编码已存在"));
        verify(sysPermissionMapper, never()).insert(any(SysPermission.class));
    }

    @Test
    @DisplayName("创建权限 - 自定义 parentId 和 visible 不被覆盖")
    void testCreatePermission_CustomDefaults() {
        SysPermission newPerm = new SysPermission();
        newPerm.setPermissionCode("custom:perm");
        newPerm.setParentId(5L);
        newPerm.setVisible(0);

        when(sysPermissionMapper.selectCount(any())).thenReturn(0L);
        when(sysPermissionMapper.insert(any(SysPermission.class))).thenReturn(1);

        sysPermissionService.createPermission(newPerm);

        ArgumentCaptor<SysPermission> captor = ArgumentCaptor.forClass(SysPermission.class);
        verify(sysPermissionMapper).insert(captor.capture());
        assertEquals(5L, captor.getValue().getParentId());
        assertEquals(0, captor.getValue().getVisible());
    }

    // ==================== updatePermission ====================

    @Test
    @DisplayName("更新权限 - 成功")
    void testUpdatePermission_Success() {
        SysPermission updatePerm = new SysPermission();
        updatePerm.setId(1L);
        updatePerm.setPermissionName("用户管理-修改");
        updatePerm.setPermissionCode("user:manage_v2");

        when(sysPermissionMapper.selectById(1L)).thenReturn(mockPermission);
        when(sysPermissionMapper.updateById(any(SysPermission.class))).thenReturn(1);

        sysPermissionService.updatePermission(updatePerm);

        verify(sysPermissionMapper).selectById(1L);
        verify(sysPermissionMapper).updateById(any(SysPermission.class));
    }

    @Test
    @DisplayName("更新权限 - 不存在抛异常")
    void testUpdatePermission_NotFound() {
        SysPermission updatePerm = new SysPermission();
        updatePerm.setId(999L);
        updatePerm.setPermissionName("不存在的权限");

        when(sysPermissionMapper.selectById(999L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> sysPermissionService.updatePermission(updatePerm));
        assertTrue(ex.getMessage().contains("权限不存在"));
        verify(sysPermissionMapper, never()).updateById(any(SysPermission.class));
    }

    // ==================== deletePermission ====================

    @Test
    @DisplayName("删除权限 - 成功并清理角色关联")
    void testDeletePermission_Success() {
        when(sysPermissionMapper.selectById(1L)).thenReturn(mockPermission);
        when(sysPermissionMapper.deleteById(1L)).thenReturn(1);
        when(sysRolePermissionMapper.delete(any())).thenReturn(1);

        sysPermissionService.deletePermission(1L);

        verify(sysPermissionMapper).selectById(1L);
        verify(sysPermissionMapper).deleteById(1L);
        verify(sysRolePermissionMapper).delete(any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("删除权限 - 不存在抛异常")
    void testDeletePermission_NotFound() {
        when(sysPermissionMapper.selectById(999L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> sysPermissionService.deletePermission(999L));
        assertTrue(ex.getMessage().contains("权限不存在"));
        verify(sysPermissionMapper, never()).deleteById(any());
        verify(sysRolePermissionMapper, never()).delete(any());
    }

    // ==================== assignPermissionToRole ====================

    @Test
    @DisplayName("为角色分配权限 - 成功")
    void testAssignPermissionToRole_Success() {
        when(sysRolePermissionMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(sysRolePermissionMapper.insert(any(SysRolePermission.class))).thenReturn(1);

        sysPermissionService.assignPermissionToRole(1L, 2L);

        verify(sysRolePermissionMapper).selectCount(any(LambdaQueryWrapper.class));
        ArgumentCaptor<SysRolePermission> captor = ArgumentCaptor.forClass(SysRolePermission.class);
        verify(sysRolePermissionMapper).insert(captor.capture());
        SysRolePermission rp = captor.getValue();
        assertEquals(1L, rp.getRoleId());
        assertEquals(2L, rp.getPermissionId());
        assertNotNull(rp.getCreatedAt());
    }

    @Test
    @DisplayName("为角色分配权限 - 已存在抛异常")
    void testAssignPermissionToRole_AlreadyExists() {
        when(sysRolePermissionMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> sysPermissionService.assignPermissionToRole(1L, 1L));
        assertTrue(ex.getMessage().contains("角色已拥有该权限"));
        verify(sysRolePermissionMapper, never()).insert(any(SysRolePermission.class));
    }

    // ==================== removePermissionFromRole ====================

    @Test
    @DisplayName("从角色移除权限 - 成功")
    void testRemovePermissionFromRole_Success() {
        when(sysRolePermissionMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(1);

        sysPermissionService.removePermissionFromRole(1L, 1L);

        verify(sysRolePermissionMapper).delete(any(LambdaQueryWrapper.class));
    }
}
