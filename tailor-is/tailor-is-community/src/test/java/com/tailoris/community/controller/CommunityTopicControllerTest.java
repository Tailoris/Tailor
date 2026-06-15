package com.tailoris.community.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tailoris.common.dto.PageRequest;
import com.tailoris.common.dto.PageResponse;
import com.tailoris.community.entity.CommunityTopic;
import com.tailoris.community.service.CommunityTopicService;
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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("CommunityTopicController 单元测试")
@ExtendWith(MockitoExtension.class)
class CommunityTopicControllerTest {

    private MockMvc mockMvc;

    @Mock
    private CommunityTopicService topicService;

    @InjectMocks
    private CommunityTopicController topicController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(topicController).build();
    }

    @Test
    @DisplayName("创建话题成功")
    void testCreate() throws Exception {
        CommunityTopic topic = new CommunityTopic();
        topic.setId(1L);
        topic.setTopicName("测试话题");

        when(topicService.createTopic(any(CommunityTopic.class))).thenReturn(topic);

        mockMvc.perform(post("/api/community/topic")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(topic)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("更新话题成功")
    void testUpdate() throws Exception {
        CommunityTopic topic = new CommunityTopic();
        topic.setId(1L);
        topic.setTopicName("更新话题");

        when(topicService.updateTopic(any(CommunityTopic.class))).thenReturn(topic);

        mockMvc.perform(put("/api/community/topic/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(topic)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("删除话题成功")
    void testDelete() throws Exception {
        mockMvc.perform(delete("/api/community/topic/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(topicService).deleteTopic(1L);
    }

    @Test
    @DisplayName("获取话题详情")
    void testGet() throws Exception {
        CommunityTopic topic = new CommunityTopic();
        topic.setId(1L);
        topic.setTopicName("测试话题");

        when(topicService.getTopic(1L)).thenReturn(topic);

        mockMvc.perform(get("/api/community/topic/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("获取热门话题")
    void testHot() throws Exception {
        when(topicService.listHotTopics(anyInt())).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/community/topic/hot")
                        .param("limit", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("分页查询话题列表")
    void testList() throws Exception {
        PageResponse<CommunityTopic> response = new PageResponse<>(Collections.emptyList(), 0L, 1, 20);
        when(topicService.listTopics(any(PageRequest.class), any(), any())).thenReturn(response);

        mockMvc.perform(get("/api/community/topic/list")
                        .param("pageNum", "1")
                        .param("pageSize", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
