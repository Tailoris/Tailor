package com.tailoris.community.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tailoris.community.entity.CommunityBlock;
import com.tailoris.community.service.CommunityBlockService;
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

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("CommunityBlockController 单元测试")
@ExtendWith(MockitoExtension.class)
class CommunityBlockControllerTest {

    private MockMvc mockMvc;

    @Mock
    private CommunityBlockService blockService;

    @InjectMocks
    private CommunityBlockController blockController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(blockController).build();
    }

    @Test
    @DisplayName("屏蔽用户成功")
    void testBlock() throws Exception {
        mockMvc.perform(post("/api/community/block")
                        .header("X-User-Id", 1L)
                        .param("blockedUserId", "200")
                        .param("reason", "恶意评论"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(blockService).blockUser(1L, 200L, "恶意评论");
    }

    @Test
    @DisplayName("取消屏蔽成功")
    void testUnblock() throws Exception {
        mockMvc.perform(delete("/api/community/block/200")
                        .header("X-User-Id", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(blockService).unblockUser(1L, 200L);
    }

    @Test
    @DisplayName("获取屏蔽列表")
    void testList() throws Exception {
        when(blockService.listBlocked(1L)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/community/block/list")
                        .header("X-User-Id", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
