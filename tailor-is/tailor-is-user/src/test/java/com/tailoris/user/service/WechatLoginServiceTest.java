package com.tailoris.user.service;

import com.tailoris.common.exception.BusinessException;
import com.tailoris.common.util.AuditLogUtils;
import com.tailoris.user.config.WechatProperties;
import com.tailoris.user.dto.LoginResponse;
import com.tailoris.user.dto.WechatLoginRequest;
import com.tailoris.user.entity.SysUser;
import com.tailoris.user.mapper.SysUserMapper;
import com.tailoris.user.service.impl.WechatLoginServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;

import java.util.List;

import cn.dev33.satoken.stp.StpUtil;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * WechatLoginService 补充单元测试 - TEST-P2-01.
 *
 * <p>覆盖 WechatLoginServiceImplTest 中未覆盖的场景：</p>
 * <ul>
 *   <li>微信绑定/解绑流程</li>
 *   <li>微信小程序手机号获取</li>
 *   <li>UnionID 关联</li>
 *   <li>微信登录后同步用户信息</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("WechatLoginService 补充单元测试")
class WechatLoginServiceTest {

    @Mock private SysUserMapper sysUserMapper;
    @Mock private WechatProperties wechatProperties;
    @Mock private StringRedisTemplate stringRedisTemplate;
    @Mock private SysRoleService sysRoleService;
    @Mock private SysPermissionService sysPermissionService;
    @Mock private RestClient restClient;
    @Mock private RestClient.RequestHeadersUriSpec requestHeadersUriSpec;
    @Mock private RestClient.RequestHeadersSpec requestHeadersSpec;
    @Mock private RestClient.ResponseSpec responseSpec;
    @Mock private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private WechatLoginServiceImpl wechatLoginService;

    private MockedStatic<StpUtil> stpUtilMock;
    private MockedStatic<AuditLogUtils> auditLogUtilsMock;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(wechatLoginService, "restClient", restClient);

        stpUtilMock = mockStatic(StpUtil.class);
        auditLogUtilsMock = mockStatic(AuditLogUtils.class);

        WechatProperties.Mp mpConfig = new WechatProperties.Mp();
        mpConfig.setAppId("wx_mp_appid");
        mpConfig.setAppSecret("mp_secret_123");

        WechatProperties.Mini miniConfig = new WechatProperties.Mini();
        miniConfig.setAppId("wx_mini_appid");
        miniConfig.setAppSecret("mini_secret_456");

        lenient().when(wechatProperties.getMp()).thenReturn(mpConfig);
        lenient().when(wechatProperties.getMini()).thenReturn(miniConfig);

        stpUtilMock.when(StpUtil::getLoginIdAsLong).thenReturn(1L);
        stpUtilMock.when(StpUtil::getTokenValue).thenReturn("mock-token");
        lenient().when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @AfterEach
    void tearDown() {
        stpUtilMock.close();
        auditLogUtilsMock.close();
    }

    // ============================================================
    // 微信绑定
    // ============================================================

    @Test
    @DisplayName("绑定微信 - 成功")
    void bindWechat_Success() {
        Long userId = 1L;
        WechatLoginRequest request = new WechatLoginRequest();
        request.setType("MP");
        request.setCode("bind_code");

        WechatLoginServiceImpl.WechatTokenResponse tokenResp = buildTokenResponse("openid_bind", "unionid_bind", "token");
        setupRestClientMock(tokenResp);

        when(sysUserMapper.selectOne(any())).thenReturn(null);
        when(sysUserMapper.selectById(userId)).thenReturn(buildUser(userId));
        when(sysUserMapper.updateById(any(SysUser.class))).thenReturn(1);

        wechatLoginService.bindWechat(userId, request);

        verify(sysUserMapper).updateById(any(SysUser.class));
    }

    @Test
    @DisplayName("绑定微信 - 已绑定其他账号抛异常")
    void bindWechat_AlreadyBound() {
        Long userId = 1L;
        WechatLoginRequest request = new WechatLoginRequest();
        request.setType("MP");
        request.setCode("bind_code");

        WechatLoginServiceImpl.WechatTokenResponse tokenResp = buildTokenResponse("openid_bound", "unionid", "token");
        setupRestClientMock(tokenResp);

        SysUser existingUser = new SysUser();
        existingUser.setId(999L);
        existingUser.setUsername("other_user");
        when(sysUserMapper.selectOne(any())).thenReturn(existingUser);

        assertThrows(BusinessException.class,
                () -> wechatLoginService.bindWechat(userId, request));
    }

    @Test
    @DisplayName("绑定微信 - 用户不存在抛异常")
    void bindWechat_UserNotFound() {
        WechatLoginRequest request = new WechatLoginRequest();
        request.setType("MP");
        request.setCode("bind_code");

        WechatLoginServiceImpl.WechatTokenResponse tokenResp = buildTokenResponse("openid", "unionid", "token");
        setupRestClientMock(tokenResp);

        when(sysUserMapper.selectOne(any())).thenReturn(null);
        when(sysUserMapper.selectById(999L)).thenReturn(null);

        assertThrows(BusinessException.class,
                () -> wechatLoginService.bindWechat(999L, request));
    }

    // ============================================================
    // 微信解绑
    // ============================================================

    @Test
    @DisplayName("解绑微信 - 成功")
    void unbindWechat_Success() {
        Long userId = 1L;
        SysUser user = buildUser(userId);
        user.setWxOpenid("wx_openid_old");
        when(sysUserMapper.selectById(userId)).thenReturn(user);
        when(sysUserMapper.updateById(any(SysUser.class))).thenReturn(1);

        wechatLoginService.unbindWechat(userId);

        ArgumentCaptor<SysUser> captor = org.mockito.ArgumentCaptor.forClass(SysUser.class);
        verify(sysUserMapper).updateById(captor.capture());
        assertNull(captor.getValue().getWxOpenid());
    }

    @Test
    @DisplayName("解绑微信 - 未绑定抛异常")
    void unbindWechat_NotBound() {
        Long userId = 1L;
        SysUser user = buildUser(userId);
        when(sysUserMapper.selectById(userId)).thenReturn(user);

        assertThrows(BusinessException.class,
                () -> wechatLoginService.unbindWechat(userId));
    }

    // ============================================================
    // 小程序手机号获取
    // ============================================================

    @Test
    @DisplayName("获取微信手机号 - 成功")
    void getWxPhoneNumber_Success() {
        String code = "phone_code";
        String expectedPhone = "13800138000";

        WechatLoginServiceImpl.WechatTokenResponse tokenResp = new WechatLoginServiceImpl.WechatTokenResponse();
        tokenResp.setOpenid("openid");
        tokenResp.setAccessToken("access_token");
        setupRestClientMock(tokenResp);

        when(valueOperations.get(anyString())).thenReturn("access_token");

        // Mock phone number response
        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);

        WechatLoginServiceImpl.WxPhoneResponse phoneResp = new WechatLoginServiceImpl.WxPhoneResponse();
        phoneResp.setPhoneNumber(expectedPhone);
        when(responseSpec.body(WechatLoginServiceImpl.WxPhoneResponse.class)).thenReturn(phoneResp);

        String phone = wechatLoginService.getWxPhoneNumber(code);

        assertEquals(expectedPhone, phone);
    }

    @Test
    @DisplayName("获取微信手机号 - code为空抛异常")
    void getWxPhoneNumber_EmptyCode() {
        assertThrows(BusinessException.class,
                () -> wechatLoginService.getWxPhoneNumber(""));
    }

    // ============================================================
    // 微信登录后同步用户信息
    // ============================================================

    @Test
    @DisplayName("同步微信用户信息 - 成功")
    void syncWechatUserInfo_Success() {
        Long userId = 1L;
        SysUser user = buildUser(userId);
        when(sysUserMapper.selectById(userId)).thenReturn(user);
        when(sysUserMapper.updateById(any(SysUser.class))).thenReturn(1);

        wechatLoginService.syncWechatUserInfo(userId, "https://avatar.url", "微信昵称");

        ArgumentCaptor<SysUser> captor = org.mockito.ArgumentCaptor.forClass(SysUser.class);
        verify(sysUserMapper).updateById(captor.capture());
        assertEquals("微信昵称", captor.getValue().getNickName());
        assertEquals("https://avatar.url", captor.getValue().getAvatar());
    }

    @Test
    @DisplayName("同步微信用户信息 - 用户不存在不抛异常")
    void syncWechatUserInfo_UserNotFound() {
        when(sysUserMapper.selectById(999L)).thenReturn(null);

        assertDoesNotThrow(() ->
                wechatLoginService.syncWechatUserInfo(999L, "avatar", "nick"));
    }

    // ============================================================
    // 辅助方法
    // ============================================================

    private SysUser buildUser(Long id) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setUsername("testuser");
        user.setStatus(1);
        return user;
    }

    private void setupRestClientMock(WechatLoginServiceImpl.WechatTokenResponse response) {
        lenient().when(restClient.get()).thenReturn(requestHeadersUriSpec);
        lenient().when(requestHeadersUriSpec.uri(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(requestHeadersSpec);
        lenient().when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        lenient().when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        lenient().when(responseSpec.body(WechatLoginServiceImpl.WechatTokenResponse.class)).thenReturn(response);
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