package com.tailoris.marketing.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tailoris.marketing.dto.MemberLevelRequest;
import com.tailoris.marketing.entity.MemberLevel;
import com.tailoris.marketing.entity.ShopMember;
import com.tailoris.marketing.service.MemberService;
import org.junit.jupiter.api.AfterEach;
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

import java.util.Arrays;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("MemberController 单元测试")
@ExtendWith(MockitoExtension.class)
class MemberControllerTest {

    @Mock
    private MemberService memberService;

    @InjectMocks
    private MemberController memberController;

    private MockMvc mockMvc;
    private MockedStatic<StpUtil> stpUtilMock;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(memberController).build();
        stpUtilMock = mockStatic(StpUtil.class);
    }

    @AfterEach
    void tearDown() {
        stpUtilMock.close();
    }

    @Test
    @DisplayName("查询会员等级体系：成功")
    void testGetMemberLevels_Success() throws Exception {
        MemberLevel level = new MemberLevel();
        level.setId(1L);
        level.setLevelName("普通会员");

        when(memberService.getMemberBenefits()).thenReturn(Arrays.asList(level));

        mockMvc.perform(get("/api/marketing/member/levels"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].levelName").value("普通会员"));
    }

    @Test
    @DisplayName("升级会员：成功")
    void testUpgradeMember_Success() throws Exception {
        stpUtilMock.when(StpUtil::isLogin).thenReturn(true);
        stpUtilMock.when(StpUtil::getLoginIdAsLong).thenReturn(1L);

        doNothing().when(memberService).upgradeMember(anyLong());

        mockMvc.perform(post("/api/marketing/member/upgrade"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("设置店铺会员：成功")
    void testSetShopMember_Success() throws Exception {
        MemberLevelRequest request = new MemberLevelRequest();
        request.setUserId(1L);
        request.setShopId(1L);
        request.setLevel(2);

        doNothing().when(memberService).setShopMember(any(MemberLevelRequest.class));

        mockMvc.perform(post("/api/marketing/member/shop/set")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("查询店铺会员信息：成功")
    void testGetShopMember_Success() throws Exception {
        stpUtilMock.when(StpUtil::isLogin).thenReturn(true);
        stpUtilMock.when(StpUtil::getLoginIdAsLong).thenReturn(1L);

        ShopMember shopMember = new ShopMember();
        shopMember.setId(1L);
        shopMember.setUserId(1L);
        shopMember.setShopId(1L);
        shopMember.setLevel(2);

        when(memberService.getShopMember(anyLong(), anyLong())).thenReturn(shopMember);

        mockMvc.perform(get("/api/marketing/member/shop/info")
                        .param("shopId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.level").value(2));
    }
}
