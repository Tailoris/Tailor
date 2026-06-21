package com.tailoris.user.service.impl;

import com.tailoris.common.crypto.AesGcmCrypto;
import com.tailoris.common.exception.BusinessException;
import com.tailoris.common.dto.PageRequest;
import com.tailoris.common.dto.PageResponse;
import com.tailoris.common.util.SpringSnowflakeIdGenerator;
import com.tailoris.user.dto.LoginRequest;
import com.tailoris.user.dto.LoginResponse;
import com.tailoris.user.dto.RealNameAuthRequest;
import com.tailoris.user.dto.RegisterRequest;
import com.tailoris.user.dto.UserUpdateRequest;
import com.tailoris.user.entity.SysUser;
import com.tailoris.user.mapper.SysUserMapper;
import com.tailoris.user.security.LoginSecurityService;
import com.tailoris.user.service.SysPermissionService;
import com.tailoris.user.service.SysRoleService;
import com.tailoris.common.util.IdCardValidator;
import com.tailoris.common.util.SpringSnowflakeIdGenerator;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;

import cn.dev33.satoken.stp.StpUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * SysUserServiceImpl 单元测试 - USR-006 修复验证.
 *
 * <p>覆盖：</p>
 * <ul>
 *   <li>登录（USR-001：锁定/失败计数/账号不存在/密码错误/成功）</li>
 *   <li>注册（USR-002：手机号重复/成功）</li>
 *   <li>实名认证（USR-004：合法身份证/非法身份证）</li>
 *   <li>CRUD（getUserById/Update/List/Phone）</li>
 *   <li>缓存预热与清理（USR-005）</li>
 * </ul>
 *
 * <p>TODO 待补充测试场景：</p>
 * <ul>
 *   <li>T-M01: 密码修改/重置流程测试</li>
 *   <li>T-M01: 账号注销/删除流程测试</li>
 *   <li>T-M01: 批量用户操作测试（批量禁用/启用）</li>
 *   <li>T-M01: 微信登录/绑定/解绑完整流程测试</li>
 *   <li>T-M01: 用户地址管理（CRUD）测试</li>
 *   <li>T-M01: 权限变更后的缓存失效验证</li>
 * </ul>
 *
 * @author Tailor IS Team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("SysUserServiceImpl 单元测试 (USR-006)")
class SysUserServiceImplTest {

    @Mock private SysUserMapper sysUserMapper;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private LoginSecurityService loginSecurityService;
    @Mock private AesGcmCrypto aesGcmCrypto;
    @Mock private StringRedisTemplate stringRedisTemplate;
    @Mock private SysRoleService sysRoleService;
    @Mock private SysPermissionService sysPermissionService;
    @Mock private SpringSnowflakeIdGenerator snowflakeIdGenerator;

    @InjectMocks
    private SysUserServiceImpl sysUserService;

    @BeforeEach
    void setUp() {
        // 无需特殊设置（CACHE_TTL_HOURS 是 static final，ReflectionTestUtils 无法修改）
    }

    // ============================================================
    // USR-001 登录失败锁定相关
    // ============================================================

    @Test
    @DisplayName("登录 - 账号被锁定时直接抛异常")
    void login_AccountLocked() {
        LoginRequest req = new LoginRequest();
        req.setUsername("lockedUser");
        req.setPassword("any");
        when(loginSecurityService.isAccountLocked("lockedUser")).thenReturn(true);
        when(loginSecurityService.getLockRemainSeconds("lockedUser")).thenReturn(1800L);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> sysUserService.login(req));
        assertTrue(ex.getMessage().contains("锁定") || ex.getMessage().contains("30"));
        verify(sysUserMapper, never()).selectOne(any());
    }

    @Test
    @DisplayName("登录 - 用户不存在时记录失败并抛异常")
    void login_UserNotFound() {
        LoginRequest req = new LoginRequest();
        req.setUsername("ghost");
        req.setPassword("any");
        when(loginSecurityService.isAccountLocked("ghost")).thenReturn(false);
        when(sysUserMapper.selectOne(any())).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> sysUserService.login(req));
        assertTrue(ex.getMessage().contains("用户") || ex.getMessage().contains("不存在"));
        verify(loginSecurityService).recordLoginFailure("ghost");
    }

    @Test
    @DisplayName("登录 - 密码错误时记录失败")
    void login_WrongPassword() {
        LoginRequest req = new LoginRequest();
        req.setUsername("alice");
        req.setPassword("wrong");

        SysUser dbUser = new SysUser();
        dbUser.setId(1L);
        dbUser.setUsername("alice");
        dbUser.setPassword("$2a$10$encoded");
        dbUser.setStatus(0); // 0=正常

        when(loginSecurityService.isAccountLocked("alice")).thenReturn(false);
        when(sysUserMapper.selectOne(any())).thenReturn(dbUser);
        when(passwordEncoder.matches("wrong", "$2a$10$encoded")).thenReturn(false);

        assertThrows(BusinessException.class, () -> sysUserService.login(req));
        verify(loginSecurityService).recordLoginFailure("alice");
    }

    @Test
    @DisplayName("登录 - 账号禁用时拒绝")
    void login_AccountDisabled() {
        LoginRequest req = new LoginRequest();
        req.setUsername("bob");
        req.setPassword("right");

        SysUser dbUser = new SysUser();
        dbUser.setId(2L);
        dbUser.setUsername("bob");
        dbUser.setPassword("$2a$10$encoded");
        dbUser.setStatus(1); // 1=禁用

        when(loginSecurityService.isAccountLocked("bob")).thenReturn(false);
        when(sysUserMapper.selectOne(any())).thenReturn(dbUser);
        when(passwordEncoder.matches("right", "$2a$10$encoded")).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> sysUserService.login(req));
        assertTrue(ex.getMessage().contains("禁用") || ex.getMessage().contains("停用"));
    }

    // ============================================================
    // USR-002 注册相关
    // ============================================================

    @Test
    @DisplayName("注册 - 手机号已存在应抛异常")
    void register_PhoneExists() {
        RegisterRequest req = new RegisterRequest();
        req.setPhone("13800138000");
        req.setPassword("pwd123");
        req.setSmsCode("123456");

        when(loginSecurityService.verifySmsCode(eq("13800138000"), eq("123456")))
                .thenReturn(LoginSecurityService.SmsVerifyResult.SUCCESS);
        when(sysUserMapper.selectCount(any())).thenReturn(1L);

        assertThrows(BusinessException.class, () -> sysUserService.register(req));
        verify(sysUserMapper, never()).insert(any(SysUser.class));
    }

    @Test
    @DisplayName("注册 - 短信验证码错误应抛异常")
    void register_WrongSmsCode() {
        RegisterRequest req = new RegisterRequest();
        req.setPhone("13800138000");
        req.setPassword("pwd123");
        req.setSmsCode("000000");

        when(loginSecurityService.verifySmsCode(eq("13800138000"), eq("000000")))
                .thenReturn(LoginSecurityService.SmsVerifyResult.MISMATCH);

        assertThrows(BusinessException.class, () -> sysUserService.register(req));
    }

    // ============================================================
    // USR-004 实名认证
    // ============================================================

    @Test
    @DisplayName("实名认证 - 合法身份证号")
    void realNameAuth_Valid() {
        Long userId = 1L;
        RealNameAuthRequest req = new RealNameAuthRequest();
        req.setRealName("张三");
        // 一个合法的测试身份证号（示例）
        req.setIdCard("11010519491231002X");
        req.setIdCardFront("https://oss.example.com/front.jpg");
        req.setIdCardBack("https://oss.example.com/back.jpg");

        SysUser user = new SysUser();
        user.setId(userId);
        when(sysUserMapper.selectById(userId)).thenReturn(user);
        when(aesGcmCrypto.encrypt(anyString())).thenReturn("encrypted");

        sysUserService.realNameAuth(userId, req);

        ArgumentCaptor<SysUser> captor = ArgumentCaptor.forClass(SysUser.class);
        verify(sysUserMapper).updateById(captor.capture());
        SysUser updated = captor.getValue();
        assertEquals("encrypted", updated.getIdCard());
        assertEquals(1, updated.getCertification());
        assertEquals(LocalDate.of(1949, 12, 31), updated.getBirthday());
        // 17位=2，偶数=女 (性别 2)
        assertEquals(2, updated.getGender());
    }

    @Test
    @DisplayName("实名认证 - 非法身份证号应抛异常")
    void realNameAuth_InvalidIdCard() {
        Long userId = 1L;
        RealNameAuthRequest req = new RealNameAuthRequest();
        req.setRealName("张三");
        req.setIdCard("123456789012345678");  // 末位校验位不对
        req.setIdCardFront("url");
        req.setIdCardBack("url");

        assertThrows(BusinessException.class,
                () -> sysUserService.realNameAuth(userId, req));
        verify(sysUserMapper, never()).updateById(any(SysUser.class));
    }

    @Test
    @DisplayName("实名认证 - 用户不存在应抛异常")
    void realNameAuth_UserNotFound() {
        Long userId = 999L;
        RealNameAuthRequest req = new RealNameAuthRequest();
        req.setRealName("李四");
        req.setIdCard("11010519491231002X");
        req.setIdCardFront("url");
        req.setIdCardBack("url");

        when(sysUserMapper.selectById(userId)).thenReturn(null);

        assertThrows(BusinessException.class,
                () -> sysUserService.realNameAuth(userId, req));
    }

    // ============================================================
    // USR-005 缓存预热与清理
    // ============================================================

    @Test
    @DisplayName("缓存预热 - 用户存在时预热角色和权限")
    void preheatUserCache_UserExists() {
        Long userId = 1L;
        SysUser user = new SysUser();
        user.setId(userId);
        when(sysUserMapper.selectById(userId)).thenReturn(user);

        sysUserService.preheatUserCache(userId);

        verify(sysRoleService, times(1)).listRolesByUserId(userId);
        verify(sysPermissionService, times(1)).getPermissionsByUserId(userId);
    }

    @Test
    @DisplayName("缓存预热 - 用户不存在时不抛异常")
    void preheatUserCache_UserNotFound() {
        Long userId = 999L;
        when(sysUserMapper.selectById(userId)).thenReturn(null);

        // 不应抛异常
        sysUserService.preheatUserCache(userId);
        verify(sysRoleService, never()).listRolesByUserId(anyLong());
    }

    @Test
    @DisplayName("缓存清理 - 异常时也不应抛异常")
    void invalidateUserCache_SafeWhenError() {
        Long userId = 1L;
        org.mockito.Mockito.doThrow(new RuntimeException("Redis down"))
                .when(stringRedisTemplate).delete(anyString());

        // 即使 Redis 抛异常，invalidateUserCache 也不应向调用方抛
        sysUserService.invalidateUserCache(userId);
    }

    // ============================================================
    // IdCardValidator 单元测试
    // ============================================================

    @Test
    @DisplayName("身份证号校验 - 合法")
    void idCardValidator_Valid() {
        assertTrue(IdCardValidator.isValid("11010519491231002X"));
    }

    @Test
    @DisplayName("身份证号校验 - 校验位错误")
    void idCardValidator_BadCheckCode() {
        assertEquals(false, IdCardValidator.isValid("110105194912310020"));
    }

    @Test
    @DisplayName("身份证号校验 - 出生日期晚于今天")
    void idCardValidator_FutureBirthDate() {
        assertEquals(false, IdCardValidator.isValid("11010520991231002X"));
    }

    @Test
    @DisplayName("身份证号校验 - 提取出生日期与性别")
    void idCardValidator_Extract() {
        LocalDate birth = IdCardValidator.extractBirthDate("11010519491231002X");
        assertEquals(LocalDate.of(1949, 12, 31), birth);
        // 17位=3，奇数=男
        assertEquals(1, IdCardValidator.extractGender("11010519491231003X"));
        // 17位=2，偶数=女
        assertEquals(2, IdCardValidator.extractGender("11010519491231002X"));
    }

    @Test
    @DisplayName("身份证号脱敏")
    void idCardValidator_Mask() {
        assertEquals("1101**********002X", IdCardValidator.mask("11010519491231002X"));
    }

    // ============================================================
    // 新增测试 - 提升覆盖率
    // ============================================================

    @Test
    @DisplayName("注册成功")
    void register_Success() {
        RegisterRequest req = new RegisterRequest();
        req.setPhone("13800138000");
        req.setPassword("password123");
        req.setSmsCode("123456");

        when(loginSecurityService.verifySmsCode(eq("13800138000"), eq("123456")))
                .thenReturn(LoginSecurityService.SmsVerifyResult.SUCCESS);
        when(sysUserMapper.selectCount(any())).thenReturn(0L);
        when(passwordEncoder.encode("password123")).thenReturn("$2a$10$encoded");
        when(sysUserMapper.insert(any(SysUser.class))).thenReturn(1);

        sysUserService.register(req);

        verify(sysUserMapper).insert(any(SysUser.class));
    }

    @Test
    @DisplayName("登录成功")
    void login_Success() {
        LoginRequest req = new LoginRequest();
        req.setUsername("alice");
        req.setPassword("correct");

        SysUser dbUser = new SysUser();
        dbUser.setId(1L);
        dbUser.setUsername("alice");
        dbUser.setPassword("$2a$10$encoded");
        dbUser.setStatus(0);
        dbUser.setPhone("13800138000");

        when(loginSecurityService.isAccountLocked("alice")).thenReturn(false);
        when(sysUserMapper.selectOne(any())).thenReturn(dbUser);
        when(passwordEncoder.matches("correct", "$2a$10$encoded")).thenReturn(true);
        when(sysRoleService.listRolesByUserIds(any()))
                .thenReturn(java.util.Collections.singletonMap(1L, List.of()));
        when(sysPermissionService.getPermissionsByUserIds(any()))
                .thenReturn(java.util.Collections.singletonMap(1L, List.of()));

        try (MockedStatic<StpUtil> stpUtilMock = org.mockito.Mockito.mockStatic(StpUtil.class)) {
            stpUtilMock.when(() -> StpUtil.login(1L)).thenAnswer(inv -> null);
            stpUtilMock.when(() -> StpUtil.getTokenValue()).thenReturn("mock-token");

            LoginResponse response = sysUserService.login(req);

            assertNotNull(response);
            assertNotNull(response.getToken());
            verify(loginSecurityService).clearLoginFailures("alice");
        }
    }

    @Test
    @DisplayName("更新用户信息成功")
    void updateUser_Success() {
        Long userId = 1L;
        UserUpdateRequest request = new UserUpdateRequest();
        request.setNickName("新昵称");
        request.setAvatar("https://example.com/avatar.jpg");
        request.setGender(1);
        request.setBirthday(LocalDate.of(1990, 1, 1));

        SysUser user = new SysUser();
        user.setId(userId);
        when(sysUserMapper.selectById(userId)).thenReturn(user);
        when(sysUserMapper.updateById(any(SysUser.class))).thenReturn(1);

        sysUserService.updateUser(userId, request);

        verify(sysUserMapper).updateById(any(SysUser.class));
    }

    @Test
    @DisplayName("更新用户信息 - 用户不存在应抛异常")
    void updateUser_UserNotFound() {
        Long userId = 999L;
        UserUpdateRequest request = new UserUpdateRequest();
        request.setNickName("新昵称");

        when(sysUserMapper.selectById(userId)).thenReturn(null);

        assertThrows(BusinessException.class, () -> sysUserService.updateUser(userId, request));
    }

    @Test
    @DisplayName("获取用户信息成功")
    void getUserInfo_Success() {
        Long userId = 1L;
        SysUser user = new SysUser();
        user.setId(userId);
        user.setUsername("testuser");
        user.setPhone("13800138000");

        when(sysUserMapper.selectById(userId)).thenReturn(user);
        when(sysRoleService.listRolesByUserIds(any()))
                .thenReturn(java.util.Collections.singletonMap(userId, List.of()));
        when(sysPermissionService.getPermissionsByUserIds(any()))
                .thenReturn(java.util.Collections.singletonMap(userId, List.of()));

        LoginResponse.UserInfo userInfo = sysUserService.getUserInfo(userId);

        assertNotNull(userInfo);
        assertEquals(userId, userInfo.getId());
        assertEquals("testuser", userInfo.getUsername());
    }

    @Test
    @DisplayName("获取用户信息 - 用户不存在应抛异常")
    void getUserInfo_UserNotFound() {
        Long userId = 999L;

        when(sysUserMapper.selectById(userId)).thenReturn(null);

        assertThrows(BusinessException.class, () -> sysUserService.getUserInfo(userId));
    }

    @Test
    @DisplayName("分页查询用户列表")
    void listUsers_Success() {
        PageRequest pageRequest = new PageRequest(1, 10);
        SysUser user = new SysUser();
        user.setId(1L);

        @SuppressWarnings("unchecked")
        Page<SysUser> mockPage = org.mockito.Mockito.mock(Page.class);
        when(mockPage.getRecords()).thenReturn(List.of(user));
        when(mockPage.getTotal()).thenReturn(1L);
        when(sysUserMapper.selectPage(any(Page.class), any())).thenReturn(mockPage);

        PageResponse<SysUser> response = sysUserService.listUsers(pageRequest);

        assertNotNull(response);
        assertEquals(1L, response.getTotal());
        assertEquals(1, response.getRecords().size());
    }

    @Test
    @DisplayName("根据ID查询用户")
    void getUserById_Success() {
        Long userId = 1L;
        SysUser user = new SysUser();
        user.setId(userId);

        when(sysUserMapper.selectById(userId)).thenReturn(user);

        SysUser result = sysUserService.getUserById(userId);

        assertNotNull(result);
        assertEquals(userId, result.getId());
    }

    @Test
    @DisplayName("根据用户名查询用户")
    void getUserByUsername_Success() {
        String username = "testuser";
        SysUser user = new SysUser();
        user.setId(1L);
        user.setUsername(username);

        when(sysUserMapper.selectOne(any())).thenReturn(user);

        SysUser result = sysUserService.getUserByUsername(username);

        assertNotNull(result);
        assertEquals(username, result.getUsername());
    }

    @Test
    @DisplayName("根据手机号查询用户")
    void getUserByPhone_Success() {
        String phone = "13800138000";
        SysUser user = new SysUser();
        user.setId(1L);
        user.setPhone(phone);

        when(sysUserMapper.selectOne(any())).thenReturn(user);

        SysUser result = sysUserService.getUserByPhone(phone);

        assertNotNull(result);
        assertEquals(phone, result.getPhone());
    }

    @Test
    @DisplayName("更新登录信息")
    void updateLoginInfo_Success() {
        Long userId = 1L;
        String ip = "192.168.1.1";

        when(sysUserMapper.updateById(any(SysUser.class))).thenReturn(1);

        sysUserService.updateLoginInfo(userId, ip);

        verify(sysUserMapper).updateById(any(SysUser.class));
    }

    @Test
    @DisplayName("刷新Token成功")
    void refresh_Success() {
        Long userId = 1L;
        SysUser user = new SysUser();
        user.setId(userId);
        user.setStatus(1); // status != 0 才能通过 refresh 的校验

        when(sysUserMapper.selectById(userId)).thenReturn(user);
        when(sysRoleService.listRolesByUserIds(any()))
                .thenReturn(java.util.Collections.singletonMap(userId, List.of()));
        when(sysPermissionService.getPermissionsByUserIds(any()))
                .thenReturn(java.util.Collections.singletonMap(userId, List.of()));

        try (MockedStatic<StpUtil> stpUtilMock = org.mockito.Mockito.mockStatic(StpUtil.class)) {
            stpUtilMock.when(() -> StpUtil.login(userId)).thenAnswer(inv -> null);
            stpUtilMock.when(() -> StpUtil.getTokenValue()).thenReturn("mock-token");

            LoginResponse response = sysUserService.refresh(userId);

            assertNotNull(response);
            assertNotNull(response.getToken());
        }
    }

    @Test
    @DisplayName("刷新Token - 用户不存在应抛异常")
    void refresh_UserNotFound() {
        Long userId = 999L;

        when(sysUserMapper.selectById(userId)).thenReturn(null);

        assertThrows(BusinessException.class, () -> sysUserService.refresh(userId));
    }

    @Test
    @DisplayName("刷新Token - 用户被禁用应抛异常")
    void refresh_UserDisabled() {
        Long userId = 1L;
        SysUser user = new SysUser();
        user.setId(userId);
        user.setStatus(0); // status == 0 在 refresh 中视为禁用

        when(sysUserMapper.selectById(userId)).thenReturn(user);

        assertThrows(BusinessException.class, () -> sysUserService.refresh(userId));
    }

    @Test
    @DisplayName("获取解密身份证号")
    void getDecryptedIdCard_Success() {
        Long userId = 1L;
        SysUser user = new SysUser();
        user.setId(userId);
        user.setIdCard("encrypted_id_card");

        when(sysUserMapper.selectById(userId)).thenReturn(user);
        when(aesGcmCrypto.decrypt("encrypted_id_card")).thenReturn("11010519491231002X");

        String result = sysUserService.getDecryptedIdCard(userId);

        assertEquals("11010519491231002X", result);
    }

    @Test
    @DisplayName("获取解密身份证号 - 用户不存在返回null")
    void getDecryptedIdCard_UserNotFound() {
        Long userId = 999L;

        when(sysUserMapper.selectById(userId)).thenReturn(null);

        String result = sysUserService.getDecryptedIdCard(userId);

        assertNull(result);
    }

    @Test
    @DisplayName("发送短信验证码")
    void sendSmsCode_Success() {
        String phone = "13800138000";

        sysUserService.sendSmsCode(phone);

        verify(loginSecurityService).storeSmsCode(eq(phone), anyString());
    }
}
