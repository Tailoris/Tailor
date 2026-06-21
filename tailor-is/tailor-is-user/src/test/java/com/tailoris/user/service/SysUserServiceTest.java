package com.tailoris.user.service;

import com.tailoris.common.crypto.AesGcmCrypto;
import com.tailoris.common.exception.BusinessException;
import com.tailoris.common.dto.PageRequest;
import com.tailoris.common.dto.PageResponse;
import com.tailoris.user.dto.LoginRequest;
import com.tailoris.user.dto.LoginResponse;
import com.tailoris.user.dto.PasswordChangeRequest;
import com.tailoris.user.dto.RegisterRequest;
import com.tailoris.user.dto.RealNameAuthRequest;
import com.tailoris.user.dto.UserUpdateRequest;
import com.tailoris.user.entity.SysUser;
import com.tailoris.user.mapper.SysUserMapper;
import com.tailoris.user.security.LoginSecurityService;
import com.tailoris.user.service.impl.SysUserServiceImpl;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.List;

import cn.dev33.satoken.stp.StpUtil;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * SysUserService 补充单元测试 - TEST-P2-01.
 *
 * <p>覆盖 SysUserServiceImplTest 中 TODO 待补充的场景：</p>
 * <ul>
 *   <li>密码修改/重置流程测试</li>
 *   <li>账号注销/删除流程测试</li>
 *   <li>批量用户操作测试（批量禁用/启用）</li>
 *   <li>权限变更后的缓存失效验证</li>
 *   <li>密码强度校验</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("SysUserService 补充单元测试")
class SysUserServiceTest {

    @Mock private SysUserMapper sysUserMapper;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private LoginSecurityService loginSecurityService;
    @Mock private AesGcmCrypto aesGcmCrypto;
    @Mock private StringRedisTemplate stringRedisTemplate;
    @Mock private SysRoleService sysRoleService;
    @Mock private SysPermissionService sysPermissionService;
    @Mock private com.tailoris.common.util.SpringSnowflakeIdGenerator snowflakeIdGenerator;

    @InjectMocks
    private SysUserServiceImpl sysUserService;

    private SysUser buildUser(Long id, String username, Integer status) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setUsername(username);
        user.setPhone("13800138000");
        user.setPassword("$2a$10$encoded");
        user.setStatus(status != null ? status : 1);
        user.setNickName("用户" + username);
        return user;
    }

    // ============================================================
    // 密码修改/重置相关
    // ============================================================

    @Test
    @DisplayName("修改密码 - 成功")
    void changePassword_Success() {
        Long userId = 1L;
        SysUser user = buildUser(userId, "testuser", 1);
        when(sysUserMapper.selectById(userId)).thenReturn(user);
        when(passwordEncoder.matches("oldPassword", "$2a$10$encoded")).thenReturn(true);
        when(passwordEncoder.encode("newPassword")).thenReturn("$2a$10$newEncoded");
        when(sysUserMapper.updateById(any(SysUser.class))).thenReturn(1);

        sysUserService.changePassword(userId, "oldPassword", "newPassword");

        ArgumentCaptor<SysUser> captor = ArgumentCaptor.forClass(SysUser.class);
        verify(sysUserMapper).updateById(captor.capture());
        assertEquals("$2a$10$newEncoded", captor.getValue().getPassword());
    }

    @Test
    @DisplayName("修改密码 - 用户不存在抛异常")
    void changePassword_UserNotFound() {
        Long userId = 999L;
        when(sysUserMapper.selectById(userId)).thenReturn(null);

        assertThrows(BusinessException.class,
                () -> sysUserService.changePassword(userId, "old", "new"));
    }

    @Test
    @DisplayName("修改密码 - 旧密码错误抛异常")
    void changePassword_WrongOldPassword() {
        Long userId = 1L;
        SysUser user = buildUser(userId, "testuser", 1);
        when(sysUserMapper.selectById(userId)).thenReturn(user);
        when(passwordEncoder.matches("wrongOld", "$2a$10$encoded")).thenReturn(false);

        assertThrows(BusinessException.class,
                () -> sysUserService.changePassword(userId, "wrongOld", "new"));
        verify(sysUserMapper, never()).updateById(any(SysUser.class));
    }

    @Test
    @DisplayName("修改密码 - 新密码与旧密码相同抛异常")
    void changePassword_SamePassword() {
        Long userId = 1L;
        SysUser user = buildUser(userId, "testuser", 1);
        when(sysUserMapper.selectById(userId)).thenReturn(user);
        when(passwordEncoder.matches("same", "$2a$10$encoded")).thenReturn(true);

        assertThrows(BusinessException.class,
                () -> sysUserService.changePassword(userId, "same", "same"));
        verify(sysUserMapper, never()).updateById(any(SysUser.class));
    }

    @Test
    @DisplayName("重置密码 - 成功")
    void resetPassword_Success() {
        Long userId = 1L;
        SysUser user = buildUser(userId, "testuser", 1);
        when(sysUserMapper.selectById(userId)).thenReturn(user);
        when(passwordEncoder.encode("defaultPwd")).thenReturn("$2a$10$defaultEncoded");
        when(sysUserMapper.updateById(any(SysUser.class))).thenReturn(1);

        sysUserService.resetPassword(userId);

        ArgumentCaptor<SysUser> captor = ArgumentCaptor.forClass(SysUser.class);
        verify(sysUserMapper).updateById(captor.capture());
        assertEquals("$2a$10$defaultEncoded", captor.getValue().getPassword());
    }

    @Test
    @DisplayName("重置密码 - 用户不存在抛异常")
    void resetPassword_UserNotFound() {
        Long userId = 999L;
        when(sysUserMapper.selectById(userId)).thenReturn(null);

        assertThrows(BusinessException.class,
                () -> sysUserService.resetPassword(userId));
    }

    // ============================================================
    // 账号注销/删除相关
    // ============================================================

    @Test
    @DisplayName("注销账号 - 成功")
    void deactivateAccount_Success() {
        Long userId = 1L;
        SysUser user = buildUser(userId, "testuser", 1);
        when(sysUserMapper.selectById(userId)).thenReturn(user);
        when(sysUserMapper.updateById(any(SysUser.class))).thenReturn(1);

        sysUserService.deactivateAccount(userId);

        ArgumentCaptor<SysUser> captor = ArgumentCaptor.forClass(SysUser.class);
        verify(sysUserMapper).updateById(captor.capture());
        assertEquals(0, captor.getValue().getStatus());
    }

    @Test
    @DisplayName("注销账号 - 已注销抛异常")
    void deactivateAccount_AlreadyDeactivated() {
        Long userId = 1L;
        SysUser user = buildUser(userId, "testuser", 0);
        when(sysUserMapper.selectById(userId)).thenReturn(user);

        assertThrows(BusinessException.class,
                () -> sysUserService.deactivateAccount(userId));
        verify(sysUserMapper, never()).updateById(any(SysUser.class));
    }

    @Test
    @DisplayName("启用账号 - 成功")
    void activateAccount_Success() {
        Long userId = 1L;
        SysUser user = buildUser(userId, "testuser", 0);
        when(sysUserMapper.selectById(userId)).thenReturn(user);
        when(sysUserMapper.updateById(any(SysUser.class))).thenReturn(1);

        sysUserService.activateAccount(userId);

        ArgumentCaptor<SysUser> captor = ArgumentCaptor.forClass(SysUser.class);
        verify(sysUserMapper).updateById(captor.capture());
        assertEquals(1, captor.getValue().getStatus());
    }

    @Test
    @DisplayName("批量禁用用户 - 成功")
    void batchDisableUsers_Success() {
        List<Long> userIds = List.of(1L, 2L, 3L);
        when(sysUserMapper.updateById(any(SysUser.class))).thenReturn(1);

        sysUserService.batchDisableUsers(userIds);

        verify(sysUserMapper, times(3)).updateById(any(SysUser.class));
    }

    @Test
    @DisplayName("批量启用用户 - 成功")
    void batchEnableUsers_Success() {
        List<Long> userIds = List.of(1L, 2L);
        when(sysUserMapper.updateById(any(SysUser.class))).thenReturn(1);

        sysUserService.batchEnableUsers(userIds);

        verify(sysUserMapper, times(2)).updateById(any(SysUser.class));
    }

    @Test
    @DisplayName("批量禁用用户 - 空列表不操作")
    void batchDisableUsers_EmptyList() {
        sysUserService.batchDisableUsers(List.of());

        verify(sysUserMapper, never()).updateById(any(SysUser.class));
    }

    // ============================================================
    // 权限变更后缓存失效
    // ============================================================

    @Test
    @DisplayName("更新用户角色 - 成功后清除缓存")
    void updateUserRoles_Success() {
        Long userId = 1L;
        List<Long> roleIds = List.of(1L, 2L);
        SysUser user = buildUser(userId, "testuser", 1);
        when(sysUserMapper.selectById(userId)).thenReturn(user);
        when(sysRoleService.updateUserRoles(userId, roleIds)).thenReturn(true);

        sysUserService.updateUserRoles(userId, roleIds);

        verify(sysRoleService).updateUserRoles(userId, roleIds);
        verify(stringRedisTemplate, atLeastOnce()).delete(anyString());
    }

    @Test
    @DisplayName("更新用户角色 - 用户不存在抛异常")
    void updateUserRoles_UserNotFound() {
        Long userId = 999L;
        when(sysUserMapper.selectById(userId)).thenReturn(null);

        assertThrows(BusinessException.class,
                () -> sysUserService.updateUserRoles(userId, List.of(1L)));
    }

    // ============================================================
    // 密码强度校验
    // ============================================================

    @Test
    @DisplayName("密码强度校验 - 弱密码")
    void validatePasswordStrength_Weak() {
        assertFalse(sysUserService.validatePasswordStrength("123456"));
    }

    @Test
    @DisplayName("密码强度校验 - 中密码")
    void validatePasswordStrength_Medium() {
        assertFalse(sysUserService.validatePasswordStrength("abc12345"));
    }

    @Test
    @DisplayName("密码强度校验 - 强密码")
    void validatePasswordStrength_Strong() {
        assertTrue(sysUserService.validatePasswordStrength("MyP@ssw0rd"));
    }

    @Test
    @DisplayName("密码强度校验 - 空密码")
    void validatePasswordStrength_Empty() {
        assertFalse(sysUserService.validatePasswordStrength(""));
    }

    @Test
    @DisplayName("密码强度校验 - 短密码")
    void validatePasswordStrength_TooShort() {
        assertFalse(sysUserService.validatePasswordStrength("Ab1!"));
    }

    // ============================================================
    // 用户统计
    // ============================================================

    @Test
    @DisplayName("获取用户总数")
    void countUsers_Success() {
        when(sysUserMapper.selectCount(any())).thenReturn(100L);

        long count = sysUserService.countUsers();

        assertEquals(100L, count);
    }

    @Test
    @DisplayName("获取今日新增用户数")
    void countTodayNewUsers_Success() {
        when(sysUserMapper.selectCount(any())).thenReturn(5L);

        long count = sysUserService.countTodayNewUsers();

        assertEquals(5L, count);
    }
}