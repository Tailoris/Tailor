package com.tailoris.user.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tailoris.common.exception.GlobalExceptionHandler;
import com.tailoris.common.result.Result;
import com.tailoris.user.dto.LoginRequest;
import com.tailoris.user.dto.LoginResponse;
import com.tailoris.user.dto.RegisterRequest;
import com.tailoris.user.dto.SendSmsCodeRequest;
import com.tailoris.user.dto.WechatLoginRequest;
import com.tailoris.user.service.SysUserService;
import com.tailoris.user.service.WechatLoginService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthController 测试")
class AuthControllerTest {

    @Mock
    private SysUserService sysUserService;

    @Mock
    private WechatLoginService wechatLoginService;

    @InjectMocks
    private AuthController authController;

    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    private LoginRequest loginRequest;
    private RegisterRequest registerRequest;
    private LoginResponse loginResponse;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setValidator(validator)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        loginRequest = new LoginRequest();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("Abc123456");

        registerRequest = new RegisterRequest();
        registerRequest.setPhone("13800138000");
        registerRequest.setPassword("Abc123456");
        registerRequest.setSmsCode("123456");

        loginResponse = new LoginResponse();
        loginResponse.setToken("mock-jwt-token");
        LoginResponse.UserInfo userInfo = new LoginResponse.UserInfo();
        userInfo.setId(1L);
        userInfo.setUsername("testuser");
        loginResponse.setUserInfo(userInfo);
    }

    @Nested
    @DisplayName("登录接口测试")
    class LoginTests {

        @Test
    @DisplayName("正常登录成功")
    void testLoginSuccess() throws Exception {
        when(sysUserService.login(any(LoginRequest.class))).thenReturn(loginResponse);
        doNothing().when(sysUserService).updateLoginInfo(anyLong(), anyString());

        try (MockedStatic<StpUtil> stpUtilMock = org.mockito.Mockito.mockStatic(StpUtil.class)) {
            stpUtilMock.when(StpUtil::getLoginIdAsLong).thenReturn(1L);

            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.token").value("mock-jwt-token"));
        }
    }

    @Test
    @DisplayName("登录失败返回错误")
    void testLoginFailure() throws Exception {
        when(sysUserService.login(any(LoginRequest.class)))
                .thenThrow(new com.tailoris.common.exception.BusinessException("用户名或密码错误"));

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("用户名或密码错误"));
    }
    }

    @Nested
    @DisplayName("注册接口测试")
    class RegisterTests {

        @Test
        @DisplayName("正常注册成功")
        void testRegisterSuccess() throws Exception {
            doNothing().when(sysUserService).register(any(RegisterRequest.class));

            mockMvc.perform(post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(registerRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));
        }

        @Test
        @DisplayName("手机号已存在注册失败")
        void testRegisterPhoneExists() throws Exception {
            doThrow(new com.tailoris.common.exception.BusinessException("手机号已注册"))
                    .when(sysUserService).register(any(RegisterRequest.class));

            mockMvc.perform(post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(registerRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("手机号已注册"));
        }

        @Test
        @DisplayName("注册参数校验失败-缺少必填字段")
        void testRegisterValidationFailure() throws Exception {
            RegisterRequest invalidRequest = new RegisterRequest();

            mockMvc.perform(post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(400))
                    .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("参数校验失败")));
        }
    }

    @Nested
    @DisplayName("登出接口测试")
    class LogoutTests {

        @Test
        @DisplayName("正常登出成功")
        void testLogoutSuccess() throws Exception {
            try (MockedStatic<StpUtil> stpUtilMock = org.mockito.Mockito.mockStatic(StpUtil.class)) {
                stpUtilMock.when(StpUtil::isLogin).thenReturn(false);

                mockMvc.perform(post("/api/auth/logout"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.code").value(200));
            }
        }
    }

    @Nested
    @DisplayName("微信登录接口测试")
    class WechatLoginTests {

        @Test
        @DisplayName("微信登录成功")
        void testWechatLoginSuccess() throws Exception {
            WechatLoginRequest request = new WechatLoginRequest();
            request.setType("MP");
            request.setCode("test_code");

            when(wechatLoginService.loginByWechat(any(WechatLoginRequest.class))).thenReturn(loginResponse);

            mockMvc.perform(post("/api/auth/wechat-login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.token").value("mock-jwt-token"));
        }
    }

    @Nested
    @DisplayName("短信验证码接口测试")
    class SendSmsCodeTests {

        @Test
        @DisplayName("发送短信验证码成功")
        void testSendSmsCodeSuccess() throws Exception {
            SendSmsCodeRequest request = new SendSmsCodeRequest();
            request.setPhone("13800138000");

            doNothing().when(sysUserService).sendSmsCode("13800138000");

            mockMvc.perform(post("/api/auth/sms-code")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));
        }
    }

    @Nested
    @DisplayName("刷新Token接口测试")
    class RefreshTokenTests {

        @Test
        @DisplayName("未登录时刷新Token失败")
        void testRefreshTokenNotLoggedIn() throws Exception {
            try (MockedStatic<StpUtil> stpUtilMock = org.mockito.Mockito.mockStatic(StpUtil.class)) {
                stpUtilMock.when(() -> StpUtil.getTokenValue()).thenReturn(null);

                mockMvc.perform(post("/api/auth/refresh"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Token")));
            }
        }

        @Test
        @DisplayName("Token无效时刷新失败")
        void testRefreshTokenInvalidToken() throws Exception {
            try (MockedStatic<StpUtil> stpUtilMock = org.mockito.Mockito.mockStatic(StpUtil.class)) {
                stpUtilMock.when(() -> StpUtil.getTokenValue()).thenReturn("invalid_token");
                stpUtilMock.when(() -> StpUtil.getLoginIdByToken("invalid_token")).thenReturn(null);

                mockMvc.perform(post("/api/auth/refresh"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Token")));
            }
        }

        @Test
        @DisplayName("刷新Token成功")
        void testRefreshTokenSuccess() throws Exception {
            try (MockedStatic<StpUtil> stpUtilMock = org.mockito.Mockito.mockStatic(StpUtil.class)) {
                stpUtilMock.when(() -> StpUtil.getTokenValue()).thenReturn("valid_token");
                stpUtilMock.when(() -> StpUtil.getLoginIdByToken("valid_token")).thenReturn(1L);
                stpUtilMock.when(() -> { StpUtil.logoutByTokenValue("valid_token"); }).thenAnswer(inv -> null);

                when(sysUserService.refresh(1L)).thenReturn(loginResponse);

                mockMvc.perform(post("/api/auth/refresh"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.code").value(200))
                        .andExpect(jsonPath("$.data.token").value("mock-jwt-token"));
            }
        }
    }
}
