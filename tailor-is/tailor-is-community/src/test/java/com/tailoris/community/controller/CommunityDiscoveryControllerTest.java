package com.tailoris.community.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tailoris.common.dto.PageResponse;
import com.tailoris.common.result.Result;
import com.tailoris.community.entity.CommunityPost;
import com.tailoris.community.entity.CommunityTopic;
import com.tailoris.community.service.CommunityDiscoveryService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("CommunityDiscoveryController 单元测试")
@ExtendWith(MockitoExtension.class)
class CommunityDiscoveryControllerTest {

    private MockMvc mockMvc;

    @Mock
    private CommunityDiscoveryService discoveryService;

    @InjectMocks
    private CommunityDiscoveryController discoveryController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(discoveryController).build();
    }

    @Test
    @DisplayName("获取热门帖子")
    void testHot() throws Exception {
        PageResponse<CommunityPost> response = new PageResponse<>(Collections.emptyList(), 0L, 1, 20);
        when(discoveryService.hotPosts(any())).thenReturn(response);

        mockMvc.perform(get("/api/community/discover/hot")
                        .param("pageNum", "1")
                        .param("pageSize", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("获取最新帖子")
    void testLatest() throws Exception {
        PageResponse<CommunityPost> response = new PageResponse<>(Collections.emptyList(), 0L, 1, 20);
        when(discoveryService.latestPosts(any())).thenReturn(response);

        mockMvc.perform(get("/api/community/discover/latest")
                        .param("pageNum", "1")
                        .param("pageSize", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("获取关注流")
    void testFollowing() throws Exception {
        PageResponse<CommunityPost> response = new PageResponse<>(Collections.emptyList(), 0L, 1, 20);
        when(discoveryService.followingFeed(any(), any())).thenReturn(response);

        mockMvc.perform(get("/api/community/discover/following")
                        .header("X-User-Id", 1L)
                        .param("pageNum", "1")
                        .param("pageSize", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("获取话题帖子")
    void testByTopic() throws Exception {
        PageResponse<CommunityPost> response = new PageResponse<>(Collections.emptyList(), 0L, 1, 20);
        when(discoveryService.postsByTopic(any(), any())).thenReturn(response);

        mockMvc.perform(get("/api/community/discover/topic/100")
                        .param("pageNum", "1")
                        .param("pageSize", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("获取推荐流")
    void testRecommend() throws Exception {
        PageResponse<CommunityPost> response = new PageResponse<>(Collections.emptyList(), 0L, 1, 20);
        when(discoveryService.recommendFeed(any(), any())).thenReturn(response);

        mockMvc.perform(get("/api/community/discover/recommend")
                        .header("X-User-Id", 1L)
                        .param("pageNum", "1")
                        .param("pageSize", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("获取热门话题")
    void testHotTopics() throws Exception {
        Page<CommunityTopic> page = new Page<>();
        page.setRecords(Collections.emptyList());
        when(discoveryService.hotTopics(any(Integer.class))).thenReturn(page);

        mockMvc.perform(get("/api/community/discover/topics/hot")
                        .param("limit", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("获取用户主页")
    void testUserProfile() throws Exception {
        PageResponse<CommunityPost> response = new PageResponse<>(Collections.emptyList(), 0L, 1, 20);
        when(discoveryService.userProfile(any(), any())).thenReturn(response);

        mockMvc.perform(get("/api/community/discover/user/1")
                        .param("pageNum", "1")
                        .param("pageSize", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("搜索帖子")
    void testSearch() throws Exception {
        PageResponse<CommunityPost> response = new PageResponse<>(Collections.emptyList(), 0L, 1, 20);
        when(discoveryService.searchPosts(any(), any())).thenReturn(response);

        mockMvc.perform(get("/api/community/discover/search")
                        .param("keyword", "测试")
                        .param("pageNum", "1")
                        .param("pageSize", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
