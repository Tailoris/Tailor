package com.tailoris.community.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tailoris.common.dto.PageRequest;
import com.tailoris.common.dto.PageResponse;
import com.tailoris.community.dto.PostCreateRequest;
import com.tailoris.community.entity.CommunityPost;
import com.tailoris.community.service.CommunityPostService;
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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("PostController 单元测试")
@ExtendWith(MockitoExtension.class)
class PostControllerTest {

    private MockMvc mockMvc;

    @Mock
    private CommunityPostService communityPostService;

    @InjectMocks
    private PostController postController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(postController).build();
    }

    @Test
    @DisplayName("发布帖子成功")
    void testCreatePost() throws Exception {
        try (MockedStatic<StpUtil> mockStpUtil = mockStatic(StpUtil.class)) {
            mockStpUtil.when(StpUtil::isLogin).thenReturn(true);
            mockStpUtil.when(StpUtil::getLoginIdAsLong).thenReturn(1L);

            PostCreateRequest request = new PostCreateRequest();
            request.setTitle("测试帖子");
            request.setContent("测试内容");
            request.setType(1);
            request.setCategoryId(10L);

            CommunityPost post = new CommunityPost();
            post.setId(100L);
            post.setTitle("测试帖子");

            when(communityPostService.createPost(anyLong(), any(PostCreateRequest.class))).thenReturn(post);

            mockMvc.perform(post("/api/community/post/create")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));
        }
    }

    @Test
    @DisplayName("编辑帖子成功")
    void testUpdatePost() throws Exception {
        try (MockedStatic<StpUtil> mockStpUtil = mockStatic(StpUtil.class)) {
            mockStpUtil.when(StpUtil::isLogin).thenReturn(true);
            mockStpUtil.when(StpUtil::getLoginIdAsLong).thenReturn(1L);

            PostCreateRequest request = new PostCreateRequest();
            request.setTitle("更新帖子");
            request.setContent("更新内容");

            CommunityPost post = new CommunityPost();
            post.setId(100L);
            post.setTitle("更新帖子");

            when(communityPostService.updatePost(anyLong(), anyLong(), any(PostCreateRequest.class))).thenReturn(post);

            mockMvc.perform(put("/api/community/post/update/100")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));
        }
    }

    @Test
    @DisplayName("删除帖子成功")
    void testDeletePost() throws Exception {
        try (MockedStatic<StpUtil> mockStpUtil = mockStatic(StpUtil.class)) {
            mockStpUtil.when(StpUtil::isLogin).thenReturn(true);
            mockStpUtil.when(StpUtil::getLoginIdAsLong).thenReturn(1L);

            mockMvc.perform(delete("/api/community/post/delete/100"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));

            verify(communityPostService).deletePost(1L, 100L);
        }
    }

    @Test
    @DisplayName("获取帖子详情")
    void testGetPostDetail() throws Exception {
        CommunityPost post = new CommunityPost();
        post.setId(100L);
        post.setTitle("测试帖子");

        when(communityPostService.getPostDetail(anyLong())).thenReturn(post);

        mockMvc.perform(get("/api/community/post/detail/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("分页查询帖子列表")
    void testListPosts() throws Exception {
        PageResponse<CommunityPost> response = new PageResponse<>(Collections.emptyList(), 0L, 1, 10);
        when(communityPostService.listPosts(any(PageRequest.class), any(), any(), any())).thenReturn(response);

        mockMvc.perform(get("/api/community/post/list")
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("查询我的帖子列表")
    void testListMyPosts() throws Exception {
        try (MockedStatic<StpUtil> mockStpUtil = mockStatic(StpUtil.class)) {
            mockStpUtil.when(StpUtil::isLogin).thenReturn(true);
            mockStpUtil.when(StpUtil::getLoginIdAsLong).thenReturn(1L);

            PageResponse<CommunityPost> response = new PageResponse<>(Collections.emptyList(), 0L, 1, 10);
            when(communityPostService.listUserPosts(anyLong(), any(PageRequest.class))).thenReturn(response);

            mockMvc.perform(get("/api/community/post/my")
                            .param("pageNum", "1")
                            .param("pageSize", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));
        }
    }

    @Test
    @DisplayName("置顶帖子")
    void testSetTop() throws Exception {
        mockMvc.perform(post("/api/community/post/top/100")
                        .param("isTop", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(communityPostService).setTop(100L, 1);
    }

    @Test
    @DisplayName("加精帖子")
    void testSetEssence() throws Exception {
        mockMvc.perform(post("/api/community/post/essence/100")
                        .param("isEssence", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(communityPostService).setEssence(100L, 1);
    }
}
