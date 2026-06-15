package com.tailoris.marketing.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tailoris.common.dto.PageRequest;
import com.tailoris.common.dto.PageResponse;
import com.tailoris.marketing.entity.MktGroupBuy;
import com.tailoris.marketing.entity.MktGroupBuyInstance;
import com.tailoris.marketing.entity.MktGroupBuyMember;
import com.tailoris.marketing.service.MktGroupBuyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("MktGroupBuyController 单元测试")
@ExtendWith(MockitoExtension.class)
class MktGroupBuyControllerTest {

    @Mock
    private MktGroupBuyService groupBuyService;

    @InjectMocks
    private MktGroupBuyController groupBuyController;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(groupBuyController).build();
    }

    @Test
    @DisplayName("创建拼团活动：成功")
    void testCreateActivity_Success() throws Exception {
        MktGroupBuy activity = new MktGroupBuy();
        activity.setId(1L);
        activity.setActivityName("测试拼团");

        when(groupBuyService.createActivity(any(MktGroupBuy.class))).thenReturn(activity);

        mockMvc.perform(post("/api/marketing/group-buy/activity")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(activity)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.activityName").value("测试拼团"));
    }

    @Test
    @DisplayName("更新拼团活动：成功")
    void testUpdateActivity_Success() throws Exception {
        MktGroupBuy activity = new MktGroupBuy();
        activity.setActivityName("更新拼团");

        when(groupBuyService.updateActivity(any(MktGroupBuy.class))).thenReturn(activity);

        mockMvc.perform(put("/api/marketing/group-buy/activity/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(activity)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("取消拼团活动：成功")
    void testCancelActivity_Success() throws Exception {
        doNothing().when(groupBuyService).cancelActivity(1L);

        mockMvc.perform(post("/api/marketing/group-buy/activity/1/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("查询拼团活动详情：成功")
    void testGetActivityDetail_Success() throws Exception {
        MktGroupBuy activity = new MktGroupBuy();
        activity.setId(1L);
        activity.setActivityName("测试拼团");

        when(groupBuyService.getActivityDetail(1L)).thenReturn(activity);

        mockMvc.perform(get("/api/marketing/group-buy/activity/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.activityName").value("测试拼团"));
    }

    @Test
    @DisplayName("查询拼团活动列表：成功")
    void testListActivities_Success() throws Exception {
        PageResponse<MktGroupBuy> response = new PageResponse<>(Collections.emptyList(), 0, 1, 20);
        when(groupBuyService.listActivities(any(PageRequest.class), any(), any())).thenReturn(response);

        mockMvc.perform(get("/api/marketing/group-buy/activity/list")
                        .param("pageNum", "1")
                        .param("pageSize", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("开团：成功")
    void testOpenGroup_Success() throws Exception {
        MktGroupBuyInstance instance = new MktGroupBuyInstance();
        instance.setId(1L);
        instance.setCurrentSize(1);

        when(groupBuyService.openGroup(anyLong(), anyLong())).thenReturn(instance);

        mockMvc.perform(post("/api/marketing/group-buy/open")
                        .header("X-User-Id", 1L)
                        .param("activityId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("加入团：成功")
    void testJoinGroup_Success() throws Exception {
        MktGroupBuyInstance instance = new MktGroupBuyInstance();
        instance.setId(1L);
        instance.setCurrentSize(2);

        when(groupBuyService.joinGroup(anyLong(), anyLong())).thenReturn(instance);

        mockMvc.perform(post("/api/marketing/group-buy/join")
                        .header("X-User-Id", 1L)
                        .param("instanceId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("查询团详情：成功")
    void testGetInstanceDetail_Success() throws Exception {
        MktGroupBuyInstance instance = new MktGroupBuyInstance();
        instance.setId(1L);
        instance.setCurrentSize(2);

        when(groupBuyService.getInstanceDetail(1L)).thenReturn(instance);

        mockMvc.perform(get("/api/marketing/group-buy/instance/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("查询用户参与的团：成功")
    void testListUserInstances_Success() throws Exception {
        PageResponse<MktGroupBuyInstance> response = new PageResponse<>(Collections.emptyList(), 0, 1, 20);
        when(groupBuyService.listUserInstances(anyLong(), any(PageRequest.class), any())).thenReturn(response);

        mockMvc.perform(get("/api/marketing/group-buy/user/instances")
                        .header("X-User-Id", 1L)
                        .param("pageNum", "1")
                        .param("pageSize", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("查询可加入的团：成功")
    void testListJoinableGroups_Success() throws Exception {
        when(groupBuyService.listJoinableGroups(anyLong())).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/marketing/group-buy/joinable")
                        .param("activityId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("查询团成员：成功")
    void testListGroupMembers_Success() throws Exception {
        when(groupBuyService.listGroupMembers(anyLong())).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/marketing/group-buy/instance/1/members"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
