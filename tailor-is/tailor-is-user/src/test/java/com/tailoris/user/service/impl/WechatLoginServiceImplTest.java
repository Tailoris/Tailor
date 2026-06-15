package com.tailoris.user.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.tailoris.common.exception.BusinessException;
import com.tailoris.common.util.AuditLogUtils;
import com.tailoris.user.config.WechatProperties;
import com.tailoris.user.dto.LoginResponse;
import com.tailoris.user.dto.WechatLoginRequest;
import com.tailoris.user.entity.SysUser;
import com.tailoris.user.mapper.SysUserMapper;
import com.tailoris.user.service.SysPermissionService;
import com.tailoris.user.service.SysRoleService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("WechatLoginServiceImpl 单元测试 (USR-003)")
@ExtendWith(MockitoExtension.class)
class WechatLoginServiceImplTest {

    @Mock
    private SysUserMapper sysUserMapper;
    @Mock
    private WechatProperties wechatProperties;
    @Mock
    private StringRedisTemplate stringRedisTemplate;
    @Mock
    private SysRoleService sysRoleService;
    @Mock
    private SysPermissionService sysPermissionService;
    @Mock
    private RestClient restClient;
    @Mock
    private RestClient.RequestHeadersUriSpec requestHeadersUriSpec;
    @Mock
    private RestClient.RequestHeadersSpec requestHeadersSpec;
    @Mock
    private RestClient.ResponseSpec responseSpec;
    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private WechatLoginServiceImpl wechatLoginService;

    private MockedStatic<StpUtil> stpUtilMock;
    private MockedStatic<AuditLogUtils> auditLogUtilsMock;

    private WechatProperties.Mp mpConfig;
    private WechatProperties.Mini miniConfig;

    @BeforeEach
    void setUp() {
        // 注入 RestClient mock（因为原字段是 final 内联初始化）
        ReflectionTestUtils.setField(wechatLoginService, "restClient", restClient);

        stpUtilMock = mockStatic(StpUtil.class);
        auditLogUtilsMock = mockStatic(AuditLogUtils.class);

        // 配置微信属性
        mpConfig = new WechatProperties.Mp();
        mpConfig.setAppId("wx_mp_appid");
        mpConfig.setAppSecret("mp_secret_123");

        miniConfig = new WechatProperties.Mini();
        miniConfig.setAppId("wx_mini_appid");
        miniConfig.setAppSecret("mini_secret_456");

        lenient().when(wechatProperties.getMp()).thenReturn(mpConfig);
        lenient().when(wechatProperties.getMini()).thenReturn(miniConfig);

        // StpUtil mock
        stpUtilMock.when(StpUtil::getLoginIdAsLong).thenReturn(1L);
        stpUtilMock.when(StpUtil::getTokenValue).thenReturn("mock-token-xyz");

        // Redis mock
        lenient().when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @AfterEach
    void tearDown() {
        stpUtilMock.close();
        auditLogUtilsMock.close();
    }

    // ==================== 正常登录流程 ====================

    @Test
    @DisplayName("微信登录 - 公众号 H5 老用户成功")
    void loginByWechat_MP_OldUser_Success() {
        // 准备请求
        WechatLoginRequest request = new WechatLoginRequest();
        request.setType("MP");
        request.setCode("auth_code_123");

        // Mock 微信接口响应
        WechatLoginServiceImpl.WechatTokenResponse tokenResp = buildTokenResponse("openid_abc123", "unionid_xyz", "access_token_val");
        setupRestClientMock(tokenResp);

        // Mock 已存在用户
        SysUser existingUser = new SysUser();
        existingUser.setId(100L);
        existingUser.setUsername("wx_openid_abc123");
        existingUser.setNickName("微信用户_openid");
        existingUser.setStatus(1);
        existingUser.setLastLoginTime(java.time.LocalDateTime.now());

        when(sysUserMapper.selectOne(any())).thenReturn(existingUser);
        when(sysRoleService.listRolesByUserId(100L)).thenReturn(List.of());
        when(sysPermissionService.getPermissionsByUserId(100L)).thenReturn(List.of());

        // 执行
        LoginResponse response = wechatLoginService.loginByWechat(request);

        // 验证
        assertNotNull(response);
        assertEquals("mock-token-xyz", response.getToken());
        assertNotNull(response.getUserInfo());
        assertEquals(100L, response.getUserInfo().getId());
        assertFalse(response.getIsNewUser()); // 老用户

        // 验证微信接口调用
        verify(restClient).get();

        // 验证未创建新用户
        verify(sysUserMapper, never()).insert(any(SysUser.class));

        // 验证审计日志
        auditLogUtilsMock.verify(
                () -> AuditLogUtils.login(eq("100"), anyString(), eq(true), eq("wechat-oauth")),
                times(1)
        );
    }

    @Test
    @DisplayName("微信登录 - 小程序新用户成功")
    void loginByWechat_MINI_NewUser_Success() {
        WechatLoginRequest request = new WechatLoginRequest();
        request.setType("MINI");
        request.setCode("mini_code_456");

        WechatLoginServiceImpl.WechatTokenResponse tokenResp = buildTokenResponse("openid_mini_789", null, "mini_access_token");
        setupRestClientMock(tokenResp);

        // Mock 新用户（首次查询返回 null）
        when(sysUserMapper.selectOne(any())).thenReturn(null);
        when(sysUserMapper.insert(any(SysUser.class))).thenReturn(1);
        when(sysRoleService.listRolesByUserId(anyLong())).thenReturn(List.of());
        when(sysPermissionService.getPermissionsByUserId(anyLong())).thenReturn(List.of());

        LoginResponse response = wechatLoginService.loginByWechat(request);

        assertNotNull(response);
        assertTrue(response.getIsNewUser()); // 新用户

        // 验证创建了新用户
        ArgumentCaptor<SysUser> userCaptor = ArgumentCaptor.forClass(SysUser.class);
        verify(sysUserMapper).insert(userCaptor.capture());
        SysUser createdUser = userCaptor.getValue();
        assertEquals("wx_openid_mini_789", createdUser.getUsername());
        assertEquals(1, createdUser.getStatus());

        // 验证使用小程序配置（exchangeCodeForToken 中会调用 getMini() 两次: appId + appSecret）
        verify(wechatProperties, times(2)).getMini();
    }

    @Test
    @DisplayName("微信登录 - 默认类型使用公众号配置")
    void loginByWechat_DefaultType_UsesMP() {
        WechatLoginRequest request = new WechatLoginRequest();
        request.setCode("default_code");
        // type 默认是 "MP"

        WechatLoginServiceImpl.WechatTokenResponse tokenResp = buildTokenResponse("openid_default", null, "token");
        setupRestClientMock(tokenResp);

        SysUser user = new SysUser();
        user.setId(1L);
        user.setUsername("wx_openid_default");
        user.setStatus(1);
        when(sysUserMapper.selectOne(any())).thenReturn(user);
        when(sysRoleService.listRolesByUserId(1L)).thenReturn(List.of());
        when(sysPermissionService.getPermissionsByUserId(1L)).thenReturn(List.of());

        wechatLoginService.loginByWechat(request);

        // 验证使用了公众号配置
        verify(wechatProperties, times(2)).getMp();
        verify(wechatProperties, never()).getMini();
    }

    // ==================== 异常场景 ====================

    @Test
    @DisplayName("微信登录 - code 为空抛异常")
    void loginByWechat_EmptyCode() {
        WechatLoginRequest request = new WechatLoginRequest();
        request.setCode("");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> wechatLoginService.loginByWechat(request));
        assertTrue(ex.getMessage().contains("不能为空"));

        verify(restClient, never()).get();
    }

    @Test
    @DisplayName("微信登录 - request 为 null 抛异常")
    void loginByWechat_NullRequest() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> wechatLoginService.loginByWechat(null));
        assertTrue(ex.getMessage().contains("不能为空"));
    }

    @Test
    @DisplayName("微信登录 - 微信授权失败抛异常")
    void loginByWechat_WechatAuthFailure() {
        WechatLoginRequest request = new WechatLoginRequest();
        request.setCode("bad_code");

        // 模拟微信返回错误
        WechatLoginServiceImpl.WechatTokenResponse errorResp = new WechatLoginServiceImpl.WechatTokenResponse();
        errorResp.setErrcode(40029);
        errorResp.setErrmsg("invalid code");
        setupRestClientMock(errorResp);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> wechatLoginService.loginByWechat(request));
        assertTrue(ex.getMessage().contains("微信授权失败"));
        verify(sysUserMapper, never()).selectOne(any());
    }

    @Test
    @DisplayName("微信登录 - 微信接口异常返回 null 抛异常")
    void loginByWechat_WechatApiException() {
        WechatLoginRequest request = new WechatLoginRequest();
        request.setCode("code_exception");

        // 模拟 RestClient 抛异常
        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.body(WechatLoginServiceImpl.WechatTokenResponse.class))
                .thenThrow(new RuntimeException("Network error"));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> wechatLoginService.loginByWechat(request));
        assertTrue(ex.getMessage().contains("微信授权失败"));
    }

    @Test
    @DisplayName("微信登录 - 微信配置缺失抛异常")
    void loginByWechat_MissingConfig() {
        WechatLoginRequest request = new WechatLoginRequest();
        request.setCode("some_code");

        // 模拟配置缺失
        WechatProperties.Mp emptyMp = new WechatProperties.Mp();
        emptyMp.setAppId(null);
        emptyMp.setAppSecret(null);
        when(wechatProperties.getMp()).thenReturn(emptyMp);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> wechatLoginService.loginByWechat(request));
        assertTrue(ex.getMessage().contains("未配置"));
    }

    @Test
    @DisplayName("微信登录 - 账号被禁用抛异常")
    void loginByWechat_AccountDisabled() {
        WechatLoginRequest request = new WechatLoginRequest();
        request.setCode("code_disabled");

        WechatLoginServiceImpl.WechatTokenResponse tokenResp = buildTokenResponse("openid_disabled", null, "token");
        setupRestClientMock(tokenResp);

        SysUser disabledUser = new SysUser();
        disabledUser.setId(200L);
        disabledUser.setUsername("wx_openid_disabled");
        disabledUser.setStatus(0); // 禁用状态
        when(sysUserMapper.selectOne(any())).thenReturn(disabledUser);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> wechatLoginService.loginByWechat(request));
        assertTrue(ex.getMessage().contains("禁用"));
        verify(sysUserMapper, never()).insert(any(SysUser.class));
    }

    // ==================== 边界条件 ====================

    @Test
    @DisplayName("微信登录 - openid 极短用户名截断正常")
    void loginByWechat_ShortOpenid() {
        WechatLoginRequest request = new WechatLoginRequest();
        request.setType("MINI");
        request.setCode("code_short");

        WechatLoginServiceImpl.WechatTokenResponse tokenResp = buildTokenResponse("o1", null, "token");
        setupRestClientMock(tokenResp);

        when(sysUserMapper.selectOne(any())).thenReturn(null);
        when(sysUserMapper.insert(any(SysUser.class))).thenReturn(1);
        when(sysRoleService.listRolesByUserId(anyLong())).thenReturn(List.of());
        when(sysPermissionService.getPermissionsByUserId(anyLong())).thenReturn(List.of());

        LoginResponse response = wechatLoginService.loginByWechat(request);

        // 不应抛异常
        assertNotNull(response);
        ArgumentCaptor<SysUser> captor = ArgumentCaptor.forClass(SysUser.class);
        verify(sysUserMapper).insert(captor.capture());
        assertEquals("wx_o1", captor.getValue().getUsername());
    }

    @Test
    @DisplayName("微信登录 - token 缓存包含 refreshToken")
    void loginByWechat_CacheWithRefreshToken() {
        WechatLoginRequest request = new WechatLoginRequest();
        request.setCode("code_cache");

        WechatLoginServiceImpl.WechatTokenResponse tokenResp = new WechatLoginServiceImpl.WechatTokenResponse();
        tokenResp.setOpenid("openid_cache");
        tokenResp.setAccessToken("access_token_val");
        tokenResp.setRefreshToken("refresh_token_val");
        tokenResp.setExpiresIn(7200);
        setupRestClientMock(tokenResp);

        SysUser user = new SysUser();
        user.setId(300L);
        user.setUsername("wx_openid_cache");
        user.setStatus(1);
        when(sysUserMapper.selectOne(any())).thenReturn(user);
        when(sysRoleService.listRolesByUserId(300L)).thenReturn(List.of());
        when(sysPermissionService.getPermissionsByUserId(300L)).thenReturn(List.of());

        wechatLoginService.loginByWechat(request);

        // 验证 access_token 和 refresh_token 都被缓存
        verify(valueOperations, times(2)).set(anyString(), anyString(), anyLong(), any());
    }

    @Test
    @DisplayName("微信登录 - 权限加载失败不影响主流程")
    void loginByWechat_PermissionLoadFailure_Ignored() {
        WechatLoginRequest request = new WechatLoginRequest();
        request.setCode("code_perm_fail");

        WechatLoginServiceImpl.WechatTokenResponse tokenResp = buildTokenResponse("openid_perm", null, "token");
        setupRestClientMock(tokenResp);

        SysUser user = new SysUser();
        user.setId(400L);
        user.setUsername("wx_openid_perm");
        user.setStatus(1);
        when(sysUserMapper.selectOne(any())).thenReturn(user);
        when(sysRoleService.listRolesByUserId(400L)).thenThrow(new RuntimeException("Redis unavailable"));

        // 不应抛异常
        LoginResponse response = wechatLoginService.loginByWechat(request);

        assertNotNull(response);
        assertEquals(400L, response.getUserInfo().getId());
    }

    // ==================== 辅助方法 ====================

    private void setupRestClientMock(WechatLoginServiceImpl.WechatTokenResponse response) {
        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.body(WechatLoginServiceImpl.WechatTokenResponse.class)).thenReturn(response);
    }

    private WechatLoginServiceImpl.WechatTokenResponse buildTokenResponse(String openid, String unionid, String accessToken) {
        WechatLoginServiceImpl.WechatTokenResponse resp = new WechatLoginServiceImpl.WechatTokenResponse();
        resp.setOpenid(openid);
        resp.setUnionid(unionid);
        resp.setAccessToken(accessToken);
        resp.setExpiresIn(7200);
        return resp;
    }
}
