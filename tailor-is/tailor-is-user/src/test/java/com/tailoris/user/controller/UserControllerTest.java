package com.tailoris.user.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tailoris.common.dto.PageRequest;
import com.tailoris.common.dto.PageResponse;
import com.tailoris.common.exception.BusinessException;
import com.tailoris.common.exception.GlobalExceptionHandler;
import com.tailoris.user.dto.LoginResponse;
import com.tailoris.user.dto.RealNameAuthRequest;
import com.tailoris.user.dto.UserUpdateRequest;
import com.tailoris.user.entity.SysUser;
import com.tailoris.user.service.SysUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserController 单元测试")
class UserControllerTest {

    @Mock
    private SysUserService sysUserService;

    @InjectMocks
    private UserController userController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(userController)
                .setValidator(validator)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("获取用户信息成功")
    void testGetUserInfo_Success() throws Exception {
        try (MockedStatic<StpUtil> stpUtilMock = org.mockito.Mockito.mockStatic(StpUtil.class)) {
            stpUtilMock.when(StpUtil::isLogin).thenReturn(true);
            stpUtilMock.when(StpUtil::getLoginIdAsLong).thenReturn(1L);

            LoginResponse.UserInfo userInfo = new LoginResponse.UserInfo();
            userInfo.setId(1L);
            userInfo.setUsername("testuser");
            userInfo.setPhone("13800138000");

            when(sysUserService.getUserInfo(eq(1L))).thenReturn(userInfo);

            mockMvc.perform(get("/api/user/info"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.username").value("testuser"));
        }
    }

    @Test
    @DisplayName("获取用户信息 - 未登录应抛异常")
    void testGetUserInfo_NotLoggedIn() throws Exception {
        try (MockedStatic<StpUtil> stpUtilMock = org.mockito.Mockito.mockStatic(StpUtil.class)) {
            stpUtilMock.when(StpUtil::isLogin).thenReturn(false);

            mockMvc.perform(get("/api/user/info"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("用户未登录"));
        }
    }

    @Test
    @DisplayName("更新用户信息成功")
    void testUpdateUserInfo_Success() throws Exception {
        try (MockedStatic<StpUtil> stpUtilMock = org.mockito.Mockito.mockStatic(StpUtil.class)) {
            stpUtilMock.when(StpUtil::getLoginIdAsLong).thenReturn(1L);

            UserUpdateRequest request = new UserUpdateRequest();
            request.setNickName("新昵称");
            request.setAvatar("https://example.com/avatar.jpg");
            request.setGender(1);
            request.setBirthday(LocalDate.of(1990, 1, 1));

            doNothing().when(sysUserService).updateUser(eq(1L), any(UserUpdateRequest.class));

            mockMvc.perform(put("/api/user/info")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));
        }
    }

    @Test
    @DisplayName("实名认证成功")
    void testRealNameAuth_Success() throws Exception {
        try (MockedStatic<StpUtil> stpUtilMock = org.mockito.Mockito.mockStatic(StpUtil.class)) {
            stpUtilMock.when(StpUtil::getLoginIdAsLong).thenReturn(1L);

            RealNameAuthRequest request = new RealNameAuthRequest();
            request.setRealName("张三");
            request.setIdCard("11010519491231002X");
            request.setIdCardFront("https://example.com/front.jpg");
            request.setIdCardBack("https://example.com/back.jpg");

            doNothing().when(sysUserService).realNameAuth(eq(1L), any(RealNameAuthRequest.class));

            mockMvc.perform(put("/api/user/real-name-auth")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));
        }
    }

    @Test
    @DisplayName("查询用户列表成功")
    void testListUsers_Success() throws Exception {
        try (MockedStatic<StpUtil> stpUtilMock = org.mockito.Mockito.mockStatic(StpUtil.class)) {
            stpUtilMock.when(StpUtil::getLoginIdAsLong).thenReturn(1L);

            SysUser user = new SysUser();
            user.setId(1L);
            user.setUsername("testuser");

            PageResponse<SysUser> response = new PageResponse<>(List.of(user), 1L, 1, 10);
            when(sysUserService.listUsers(any(PageRequest.class))).thenReturn(response);

            mockMvc.perform(get("/api/user/list")
                    .param("pageNum", "1")
                    .param("pageSize", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.total").value(1));
        }
    }
}
