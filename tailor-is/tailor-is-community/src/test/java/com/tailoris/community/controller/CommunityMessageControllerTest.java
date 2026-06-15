package com.tailoris.community.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tailoris.community.entity.CommunityMessage;
import com.tailoris.community.service.CommunityMessageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("CommunityMessageController 单元测试")
@ExtendWith(MockitoExtension.class)
class CommunityMessageControllerTest {

    private MockMvc mockMvc;

    @Mock
    private CommunityMessageService messageService;

    @InjectMocks
    private CommunityMessageController messageController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(messageController).build();
    }

    @Test
    @DisplayName("获取消息列表")
    void testList() throws Exception {
        when(messageService.listMessages(anyLong(), anyInt())).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/community/message/list")
                        .header("X-User-Id", 1L)
                        .param("limit", "200"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("获取未读消息数")
    void testUnreadCount() throws Exception {
        when(messageService.countUnread(anyLong())).thenReturn(5L);

        mockMvc.perform(get("/api/community/message/unread-count")
                        .header("X-User-Id", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(5));
    }

    @Test
    @DisplayName("标记消息已读")
    void testMarkRead() throws Exception {
        mockMvc.perform(post("/api/community/message/100/read")
                        .header("X-User-Id", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(messageService).markAsRead(1L, 100L);
    }

    @Test
    @DisplayName("全部标记已读")
    void testReadAll() throws Exception {
        when(messageService.markAllAsRead(anyLong())).thenReturn(10);

        mockMvc.perform(post("/api/community/message/read-all")
                        .header("X-User-Id", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(10));
    }
}
