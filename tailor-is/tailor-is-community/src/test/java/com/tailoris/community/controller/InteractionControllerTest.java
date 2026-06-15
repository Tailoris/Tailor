package com.tailoris.community.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tailoris.community.dto.LikeRequest;
import com.tailoris.community.service.CommunityInteractionService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("InteractionController 单元测试")
@ExtendWith(MockitoExtension.class)
class InteractionControllerTest {

    private MockMvc mockMvc;

    @Mock
    private CommunityInteractionService communityInteractionService;

    @InjectMocks
    private InteractionController interactionController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(interactionController).build();
    }

    @Test
    @DisplayName("点赞成功")
    void testLike() throws Exception {
        try (MockedStatic<StpUtil> mockStpUtil = mockStatic(StpUtil.class)) {
            mockStpUtil.when(StpUtil::isLogin).thenReturn(true);
            mockStpUtil.when(StpUtil::getLoginIdAsLong).thenReturn(1L);

            LikeRequest request = new LikeRequest();
            request.setTargetType(1);
            request.setTargetId(100L);

            mockMvc.perform(post("/api/community/interaction/like")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));

            verify(communityInteractionService).like(1L, 1, 100L);
        }
    }

    @Test
    @DisplayName("取消点赞成功")
    void testUnlike() throws Exception {
        try (MockedStatic<StpUtil> mockStpUtil = mockStatic(StpUtil.class)) {
            mockStpUtil.when(StpUtil::isLogin).thenReturn(true);
            mockStpUtil.when(StpUtil::getLoginIdAsLong).thenReturn(1L);

            LikeRequest request = new LikeRequest();
            request.setTargetType(1);
            request.setTargetId(100L);

            mockMvc.perform(delete("/api/community/interaction/unlike")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));

            verify(communityInteractionService).unlike(1L, 1, 100L);
        }
    }

    @Test
    @DisplayName("收藏帖子成功")
    void testFavorite() throws Exception {
        try (MockedStatic<StpUtil> mockStpUtil = mockStatic(StpUtil.class)) {
            mockStpUtil.when(StpUtil::isLogin).thenReturn(true);
            mockStpUtil.when(StpUtil::getLoginIdAsLong).thenReturn(1L);

            mockMvc.perform(post("/api/community/interaction/favorite")
                            .param("postId", "100"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));

            verify(communityInteractionService).favorite(1L, 100L);
        }
    }

    @Test
    @DisplayName("取消收藏成功")
    void testUnfavorite() throws Exception {
        try (MockedStatic<StpUtil> mockStpUtil = mockStatic(StpUtil.class)) {
            mockStpUtil.when(StpUtil::isLogin).thenReturn(true);
            mockStpUtil.when(StpUtil::getLoginIdAsLong).thenReturn(1L);

            mockMvc.perform(delete("/api/community/interaction/unfavorite")
                            .param("postId", "100"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));

            verify(communityInteractionService).unfavorite(1L, 100L);
        }
    }

    @Test
    @DisplayName("关注用户成功")
    void testFollow() throws Exception {
        try (MockedStatic<StpUtil> mockStpUtil = mockStatic(StpUtil.class)) {
            mockStpUtil.when(StpUtil::isLogin).thenReturn(true);
            mockStpUtil.when(StpUtil::getLoginIdAsLong).thenReturn(1L);

            mockMvc.perform(post("/api/community/interaction/follow")
                            .param("followUserId", "200"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));

            verify(communityInteractionService).follow(1L, 200L);
        }
    }

    @Test
    @DisplayName("取消关注成功")
    void testUnfollow() throws Exception {
        try (MockedStatic<StpUtil> mockStpUtil = mockStatic(StpUtil.class)) {
            mockStpUtil.when(StpUtil::isLogin).thenReturn(true);
            mockStpUtil.when(StpUtil::getLoginIdAsLong).thenReturn(1L);

            mockMvc.perform(delete("/api/community/interaction/unfollow")
                            .param("followUserId", "200"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));

            verify(communityInteractionService).unfollow(1L, 200L);
        }
    }

    @Test
    @DisplayName("检查是否已点赞")
    void testIsLiked() throws Exception {
        try (MockedStatic<StpUtil> mockStpUtil = mockStatic(StpUtil.class)) {
            mockStpUtil.when(StpUtil::isLogin).thenReturn(true);
            mockStpUtil.when(StpUtil::getLoginIdAsLong).thenReturn(1L);
            when(communityInteractionService.isLiked(1L, 1, 100L)).thenReturn(true);

            mockMvc.perform(get("/api/community/interaction/is-liked")
                            .param("targetType", "1")
                            .param("targetId", "100"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data").value(true));
        }
    }

    @Test
    @DisplayName("检查是否已收藏")
    void testIsFavorited() throws Exception {
        try (MockedStatic<StpUtil> mockStpUtil = mockStatic(StpUtil.class)) {
            mockStpUtil.when(StpUtil::isLogin).thenReturn(true);
            mockStpUtil.when(StpUtil::getLoginIdAsLong).thenReturn(1L);
            when(communityInteractionService.isFavorite(1L, 100L)).thenReturn(true);

            mockMvc.perform(get("/api/community/interaction/is-favorited")
                            .param("postId", "100"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data").value(true));
        }
    }

    @Test
    @DisplayName("检查是否已关注")
    void testIsFollowed() throws Exception {
        try (MockedStatic<StpUtil> mockStpUtil = mockStatic(StpUtil.class)) {
            mockStpUtil.when(StpUtil::isLogin).thenReturn(true);
            mockStpUtil.when(StpUtil::getLoginIdAsLong).thenReturn(1L);
            when(communityInteractionService.isFollowed(1L, 200L)).thenReturn(true);

            mockMvc.perform(get("/api/community/interaction/is-followed")
                            .param("followUserId", "200"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data").value(true));
        }
    }

    @Test
    @DisplayName("获取收藏数")
    void testGetFavoriteCount() throws Exception {
        try (MockedStatic<StpUtil> mockStpUtil = mockStatic(StpUtil.class)) {
            mockStpUtil.when(StpUtil::isLogin).thenReturn(true);
            mockStpUtil.when(StpUtil::getLoginIdAsLong).thenReturn(1L);
            when(communityInteractionService.getFavoriteCount(1L)).thenReturn(10L);

            mockMvc.perform(get("/api/community/interaction/favorite-count"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data").value(10));
        }
    }

    @Test
    @DisplayName("获取粉丝数")
    void testGetFollowerCount() throws Exception {
        try (MockedStatic<StpUtil> mockStpUtil = mockStatic(StpUtil.class)) {
            mockStpUtil.when(StpUtil::isLogin).thenReturn(true);
            mockStpUtil.when(StpUtil::getLoginIdAsLong).thenReturn(1L);
            when(communityInteractionService.getFollowerCount(1L)).thenReturn(50L);

            mockMvc.perform(get("/api/community/interaction/follower-count"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data").value(50));
        }
    }

    @Test
    @DisplayName("获取关注数")
    void testGetFollowingCount() throws Exception {
        try (MockedStatic<StpUtil> mockStpUtil = mockStatic(StpUtil.class)) {
            mockStpUtil.when(StpUtil::isLogin).thenReturn(true);
            mockStpUtil.when(StpUtil::getLoginIdAsLong).thenReturn(1L);
            when(communityInteractionService.getFollowingCount(1L)).thenReturn(30L);

            mockMvc.perform(get("/api/community/interaction/following-count"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data").value(30));
        }
    }
}
