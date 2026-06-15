package com.tailoris.user.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.tailoris.common.exception.BusinessException;
import com.tailoris.common.exception.GlobalExceptionHandler;
import com.tailoris.user.service.SysRoleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("RoleController 单元测试")
class RoleControllerTest {

    @Mock
    private SysRoleService roleService;

    @InjectMocks
    private RoleController roleController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(roleController)
                .setValidator(validator)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("分配角色成功")
    void testAssignRoleToUser_Success() throws Exception {
        try (MockedStatic<StpUtil> stpUtilMock = org.mockito.Mockito.mockStatic(StpUtil.class)) {
            stpUtilMock.when(StpUtil::getLoginIdAsLong).thenReturn(1L);
            doNothing().when(roleService).assignRoleToUser(eq(2L), eq(1L));

            mockMvc.perform(post("/api/user/roles/2")
                    .param("roleId", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));
        }
    }

    @Test
    @DisplayName("分配角色 - 尝试修改自己的角色应失败")
    void testAssignRoleToUser_SelfModification() throws Exception {
        try (MockedStatic<StpUtil> stpUtilMock = org.mockito.Mockito.mockStatic(StpUtil.class)) {
            stpUtilMock.when(StpUtil::getLoginIdAsLong).thenReturn(1L);

            mockMvc.perform(post("/api/user/roles/1")
                    .param("roleId", "2"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("禁止修改自己的角色，请联系其他超级管理员操作"));
        }
    }

    @Test
    @DisplayName("移除角色成功")
    void testRemoveRoleFromUser_Success() throws Exception {
        try (MockedStatic<StpUtil> stpUtilMock = org.mockito.Mockito.mockStatic(StpUtil.class)) {
            stpUtilMock.when(StpUtil::getLoginIdAsLong).thenReturn(1L);
            doNothing().when(roleService).removeRoleFromUser(eq(2L), eq(1L));

            mockMvc.perform(delete("/api/user/roles/2")
                    .param("roleId", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));
        }
    }

    @Test
    @DisplayName("移除角色 - 尝试修改自己的角色应失败")
    void testRemoveRoleFromUser_SelfModification() throws Exception {
        try (MockedStatic<StpUtil> stpUtilMock = org.mockito.Mockito.mockStatic(StpUtil.class)) {
            stpUtilMock.when(StpUtil::getLoginIdAsLong).thenReturn(1L);

            mockMvc.perform(delete("/api/user/roles/1")
                    .param("roleId", "2"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("禁止修改自己的角色，请联系其他超级管理员操作"));
        }
    }
}
