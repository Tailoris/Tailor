package com.tailoris.user.service.impl;

import com.tailoris.common.exception.BusinessException;
import com.tailoris.user.entity.SysRole;
import com.tailoris.user.entity.SysUserRole;
import com.tailoris.user.mapper.SysRoleMapper;
import com.tailoris.user.mapper.SysUserRoleMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("SysRoleServiceImpl 单元测试")
@ExtendWith(MockitoExtension.class)
class SysRoleServiceImplTest {

    @Mock
    private SysRoleMapper sysRoleMapper;

    @Mock
    private SysUserRoleMapper sysUserRoleMapper;

    @InjectMocks
    private SysRoleServiceImpl sysRoleService;

    private SysRole mockRole;

    @BeforeEach
    void setUp() {
        mockRole = new SysRole();
        mockRole.setId(1L);
        mockRole.setRoleName("管理员");
        mockRole.setRoleCode("ADMIN");
        mockRole.setDescription("系统管理员角色");
        mockRole.setDataScope(1);
        mockRole.setSort(1);
        mockRole.setStatus(1);
    }

    @Test
    @DisplayName("查询所有角色列表成功")
    void testListAllRoles_Success() {
        SysRole role2 = new SysRole();
        role2.setId(2L);
        role2.setRoleName("普通用户");
        role2.setRoleCode("USER");
        role2.setSort(2);
        role2.setStatus(1);

        when(sysRoleMapper.selectList(any())).thenReturn(List.of(mockRole, role2));

        List<SysRole> roles = sysRoleService.listAllRoles();

        assertNotNull(roles);
        assertEquals(2, roles.size());
        assertEquals("ADMIN", roles.get(0).getRoleCode());
        assertEquals("USER", roles.get(1).getRoleCode());
        verify(sysRoleMapper).selectList(any());
    }

    @Test
    @DisplayName("根据ID查询角色成功")
    void testGetRoleById_Success() {
        when(sysRoleMapper.selectById(1L)).thenReturn(mockRole);

        SysRole role = sysRoleService.getRoleById(1L);

        assertNotNull(role);
        assertEquals(1L, role.getId());
        assertEquals("ADMIN", role.getRoleCode());
        assertEquals("管理员", role.getRoleName());
        verify(sysRoleMapper).selectById(1L);
    }

    @Test
    @DisplayName("创建角色成功")
    void testCreateRole_Success() {
        SysRole newRole = new SysRole();
        newRole.setRoleName("测试角色");
        newRole.setRoleCode("TEST");
        newRole.setDescription("测试用角色");
        newRole.setSort(10);

        when(sysRoleMapper.selectCount(any())).thenReturn(0L);
        when(sysRoleMapper.insert(any(SysRole.class))).thenReturn(1);

        SysRole result = sysRoleService.createRole(newRole);

        assertNotNull(result);
        assertEquals("TEST", result.getRoleCode());
        assertEquals(1, result.getStatus());
        verify(sysRoleMapper).selectCount(any());
        verify(sysRoleMapper).insert(any(SysRole.class));
    }

    @Test
    @DisplayName("更新角色成功")
    void testUpdateRole_Success() {
        SysRole updateRole = new SysRole();
        updateRole.setId(1L);
        updateRole.setRoleName("管理员-已更新");
        updateRole.setRoleCode("ADMIN_UPDATED");
        updateRole.setDescription("更新后的描述");
        updateRole.setSort(2);

        when(sysRoleMapper.selectById(1L)).thenReturn(mockRole);
        when(sysRoleMapper.updateById(any(SysRole.class))).thenReturn(1);

        sysRoleService.updateRole(updateRole);

        verify(sysRoleMapper).selectById(1L);
        verify(sysRoleMapper).updateById(any(SysRole.class));
    }

    @Test
    @DisplayName("删除角色成功")
    void testDeleteRole_Success() {
        when(sysRoleMapper.selectById(1L)).thenReturn(mockRole);
        when(sysRoleMapper.deleteById(1L)).thenReturn(1);
        when(sysUserRoleMapper.delete(any())).thenReturn(1);

        sysRoleService.deleteRole(1L);

        verify(sysRoleMapper).selectById(1L);
        verify(sysRoleMapper).deleteById(1L);
        verify(sysUserRoleMapper).delete(any());
    }

    @Test
    @DisplayName("为用户分配角色成功")
    void testAssignRoleToUser_Success() {
        Long userId = 100L;
        Long roleId = 1L;

        when(sysRoleMapper.selectById(roleId)).thenReturn(mockRole);
        when(sysUserRoleMapper.selectCount(any())).thenReturn(0L);
        when(sysUserRoleMapper.insert(any(SysUserRole.class))).thenReturn(1);

        sysRoleService.assignRoleToUser(userId, roleId);

        verify(sysRoleMapper).selectById(roleId);
        verify(sysUserRoleMapper).selectCount(any());
        verify(sysUserRoleMapper).insert(any(SysUserRole.class));
    }

    @Test
    @DisplayName("根据用户ID查询角色列表 - 用户有角色")
    void testListRolesByUserId_HasRoles() {
        Long userId = 100L;
        SysUserRole userRole = new SysUserRole();
        userRole.setUserId(userId);
        userRole.setRoleId(1L);

        when(sysUserRoleMapper.selectList(any())).thenReturn(List.of(userRole));
        when(sysRoleMapper.selectList(any())).thenReturn(List.of(mockRole));

        List<SysRole> roles = sysRoleService.listRolesByUserId(userId);

        assertNotNull(roles);
        assertEquals(1, roles.size());
        assertEquals("ADMIN", roles.get(0).getRoleCode());
    }

    @Test
    @DisplayName("根据用户ID查询角色列表 - 用户无角色")
    void testListRolesByUserId_NoRoles() {
        Long userId = 100L;

        when(sysUserRoleMapper.selectList(any())).thenReturn(List.of());

        List<SysRole> roles = sysRoleService.listRolesByUserId(userId);

        assertNotNull(roles);
        assertTrue(roles.isEmpty());
    }

    @Test
    @DisplayName("根据ID查询角色 - 角色不存在应抛异常")
    void testGetRoleById_NotFound() {
        Long roleId = 999L;

        when(sysRoleMapper.selectById(roleId)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> sysRoleService.getRoleById(roleId));
        assertTrue(ex.getMessage().contains("角色不存在"));
    }

    @Test
    @DisplayName("创建角色 - 角色编码已存在应抛异常")
    void testCreateRole_CodeExists() {
        SysRole newRole = new SysRole();
        newRole.setRoleName("测试角色");
        newRole.setRoleCode("ADMIN");

        when(sysRoleMapper.selectCount(any())).thenReturn(1L);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> sysRoleService.createRole(newRole));
        assertTrue(ex.getMessage().contains("角色编码已存在"));
        verify(sysRoleMapper, never()).insert(any(SysRole.class));
    }

    @Test
    @DisplayName("更新角色 - 角色不存在应抛异常")
    void testUpdateRole_NotFound() {
        SysRole updateRole = new SysRole();
        updateRole.setId(999L);

        when(sysRoleMapper.selectById(999L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> sysRoleService.updateRole(updateRole));
        assertTrue(ex.getMessage().contains("角色不存在"));
    }

    @Test
    @DisplayName("删除角色 - 角色不存在应抛异常")
    void testDeleteRole_NotFound() {
        Long roleId = 999L;

        when(sysRoleMapper.selectById(roleId)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> sysRoleService.deleteRole(roleId));
        assertTrue(ex.getMessage().contains("角色不存在"));
    }

    @Test
    @DisplayName("为用户分配角色 - 用户已拥有该角色应抛异常")
    void testAssignRoleToUser_AlreadyHasRole() {
        Long userId = 100L;
        Long roleId = 1L;

        when(sysRoleMapper.selectById(roleId)).thenReturn(mockRole);
        when(sysUserRoleMapper.selectCount(any())).thenReturn(1L);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> sysRoleService.assignRoleToUser(userId, roleId));
        assertTrue(ex.getMessage().contains("用户已拥有该角色"));
        verify(sysUserRoleMapper, never()).insert(any(SysUserRole.class));
    }

    @Test
    @DisplayName("从用户移除角色成功")
    void testRemoveRoleFromUser_Success() {
        Long userId = 100L;
        Long roleId = 1L;

        when(sysUserRoleMapper.delete(any())).thenReturn(1);

        sysRoleService.removeRoleFromUser(userId, roleId);

        verify(sysUserRoleMapper).delete(any());
    }
}