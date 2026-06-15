package com.tailoris.community.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tailoris.common.dto.PageRequest;
import com.tailoris.common.dto.PageResponse;
import com.tailoris.community.dto.CommentRequest;
import com.tailoris.community.entity.CommunityComment;
import com.tailoris.community.service.CommunityCommentService;
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

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("CommentController 单元测试")
@ExtendWith(MockitoExtension.class)
class CommentControllerTest {

    private MockMvc mockMvc;

    @Mock
    private CommunityCommentService communityCommentService;

    @InjectMocks
    private CommentController commentController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(commentController).build();
    }

    @Test
    @DisplayName("发表评论成功")
    void testCreateComment() throws Exception {
        try (MockedStatic<StpUtil> mockStpUtil = mockStatic(StpUtil.class)) {
            mockStpUtil.when(StpUtil::isLogin).thenReturn(true);
            mockStpUtil.when(StpUtil::getLoginIdAsLong).thenReturn(1L);

            CommentRequest request = new CommentRequest();
            request.setPostId(100L);
            request.setContent("测试评论");

            CommunityComment comment = new CommunityComment();
            comment.setId(1L);
            comment.setContent("测试评论");

            when(communityCommentService.createComment(anyLong(), any(CommentRequest.class))).thenReturn(comment);

            mockMvc.perform(post("/api/community/comment/create")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));
        }
    }

    @Test
    @DisplayName("获取帖子评论列表")
    void testListComments() throws Exception {
        PageResponse<CommunityComment> response = new PageResponse<>(Collections.emptyList(), 0L, 1, 10);
        when(communityCommentService.listComments(anyLong(), any(PageRequest.class))).thenReturn(response);

        mockMvc.perform(get("/api/community/comment/list/100")
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("删除评论成功")
    void testDeleteComment() throws Exception {
        try (MockedStatic<StpUtil> mockStpUtil = mockStatic(StpUtil.class)) {
            mockStpUtil.when(StpUtil::isLogin).thenReturn(true);
            mockStpUtil.when(StpUtil::getLoginIdAsLong).thenReturn(1L);

            mockMvc.perform(delete("/api/community/comment/delete/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));

            verify(communityCommentService).deleteComment(1L, 1L);
        }
    }

    @Test
    @DisplayName("查询我的评论列表")
    void testListMyComments() throws Exception {
        try (MockedStatic<StpUtil> mockStpUtil = mockStatic(StpUtil.class)) {
            mockStpUtil.when(StpUtil::isLogin).thenReturn(true);
            mockStpUtil.when(StpUtil::getLoginIdAsLong).thenReturn(1L);

            PageResponse<CommunityComment> response = new PageResponse<>(Collections.emptyList(), 0L, 1, 10);
            when(communityCommentService.listUserComments(anyLong(), any(PageRequest.class))).thenReturn(response);

            mockMvc.perform(get("/api/community/comment/my")
                            .param("pageNum", "1")
                            .param("pageSize", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));
        }
    }
}
