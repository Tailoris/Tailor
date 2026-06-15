package com.tailoris.community.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tailoris.common.dto.PageRequest;
import com.tailoris.common.dto.PageResponse;
import com.tailoris.community.entity.CommunityFollow;
import com.tailoris.community.entity.CommunityPost;
import com.tailoris.community.entity.CommunityPostTopic;
import com.tailoris.community.entity.CommunityTopic;
import com.tailoris.community.mapper.CommunityFollowMapper;
import com.tailoris.community.mapper.CommunityPostMapper;
import com.tailoris.community.mapper.CommunityPostTopicMapper;
import com.tailoris.community.mapper.CommunityTopicMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import com.tailoris.community.service.CommunityDiscoveryServiceImpl;

@DisplayName("CommunityDiscoveryServiceImpl 单元测试")
@ExtendWith(MockitoExtension.class)
class CommunityDiscoveryServiceImplTest {

    @Mock
    private CommunityPostMapper postMapper;

    @Mock
    private CommunityTopicMapper topicMapper;

    @Mock
    private CommunityPostTopicMapper postTopicMapper;

    @Mock
    private CommunityFollowMapper followMapper;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @InjectMocks
    private CommunityDiscoveryServiceImpl discoveryService;

    @BeforeEach
    void setUp() {
        // 通用设置
    }

    @Test
    @DisplayName("热门帖子：返回非空结果")
    void testHotPosts() {
        Page<CommunityPost> page = new Page<>();
        page.setRecords(Collections.emptyList());
        page.setTotal(0);
        when(postMapper.selectPage(any(Page.class), any())).thenReturn(page);
        PageResponse<CommunityPost> result = discoveryService.hotPosts(new PageRequest(1, 20));
        assertNotNull(result);
        assertEquals(0, result.getTotal());
    }

    @Test
    @DisplayName("最新帖子：返回分页结果")
    void testLatestPosts() {
        Page<CommunityPost> page = new Page<>();
        page.setRecords(Collections.emptyList());
        page.setTotal(0);
        when(postMapper.selectPage(any(Page.class), any())).thenReturn(page);
        PageResponse<CommunityPost> result = discoveryService.latestPosts(new PageRequest(1, 20));
        assertNotNull(result);
    }

    @Test
    @DisplayName("关注流：无关注返回空")
    void testFollowingFeed_Empty() {
        when(followMapper.selectFollowingByUser(any(Long.class))).thenReturn(Collections.emptyList());
        PageResponse<CommunityPost> result = discoveryService.followingFeed(1L, new PageRequest(1, 20));
        assertNotNull(result);
        assertEquals(0, result.getTotal());
    }

    @Test
    @DisplayName("关注流：userId 为 null 返回最新帖子")
    void testFollowingFeed_NullUserId() {
        Page<CommunityPost> page = new Page<>();
        page.setRecords(Collections.emptyList());
        page.setTotal(0);
        when(postMapper.selectPage(any(Page.class), any())).thenReturn(page);
        PageResponse<CommunityPost> result = discoveryService.followingFeed(null, new PageRequest(1, 20));
        assertNotNull(result);
    }

    @Test
    @DisplayName("关注流：有关注用户返回帖子")
    void testFollowingFeed_WithFollows() {
        CommunityFollow follow = new CommunityFollow();
        follow.setTargetUserId(2L);
        when(followMapper.selectFollowingByUser(1L)).thenReturn(List.of(follow));

        Page<CommunityPost> page = new Page<>();
        CommunityPost post = new CommunityPost();
        post.setId(100L);
        page.setRecords(List.of(post));
        page.setTotal(1);
        when(postMapper.selectPage(any(Page.class), any())).thenReturn(page);

        PageResponse<CommunityPost> result = discoveryService.followingFeed(1L, new PageRequest(1, 20));
        assertNotNull(result);
        assertEquals(1, result.getTotal());
    }

    @Test
    @DisplayName("话题帖子：topicId 为 null 返回空")
    void testPostsByTopic_NullTopicId() {
        PageResponse<CommunityPost> result = discoveryService.postsByTopic(null, new PageRequest(1, 20));
        assertNotNull(result);
        assertEquals(0, result.getTotal());
    }

    @Test
    @DisplayName("话题帖子：话题下无帖子返回空")
    void testPostsByTopic_EmptyPosts() {
        when(postTopicMapper.selectByTopicId(100L)).thenReturn(Collections.emptyList());
        PageResponse<CommunityPost> result = discoveryService.postsByTopic(100L, new PageRequest(1, 20));
        assertNotNull(result);
        assertEquals(0, result.getTotal());
    }

    @Test
    @DisplayName("话题帖子：有帖子返回列表")
    void testPostsByTopic_WithPosts() {
        CommunityPostTopic postTopic = new CommunityPostTopic();
        postTopic.setPostId(10L);
        when(postTopicMapper.selectByTopicId(100L)).thenReturn(List.of(postTopic));

        Page<CommunityPost> page = new Page<>();
        CommunityPost post = new CommunityPost();
        post.setId(10L);
        page.setRecords(List.of(post));
        page.setTotal(1);
        when(postMapper.selectPage(any(Page.class), any())).thenReturn(page);

        PageResponse<CommunityPost> result = discoveryService.postsByTopic(100L, new PageRequest(1, 20));
        assertNotNull(result);
        assertEquals(1, result.getTotal());
    }

    @Test
    @DisplayName("推荐流：返回推荐帖子")
    void testRecommendFeed() {
        Page<CommunityPost> page = new Page<>();
        CommunityPost post = new CommunityPost();
        post.setId(100L);
        page.setRecords(List.of(post));
        page.setTotal(1);
        when(postMapper.selectPage(any(Page.class), any())).thenReturn(page);

        PageResponse<CommunityPost> result = discoveryService.recommendFeed(1L, new PageRequest(1, 20));
        assertNotNull(result);
        assertEquals(1, result.getTotal());
    }

    @Test
    @DisplayName("发现话题：返回热门话题")
    void testHotTopics() {
        Page<CommunityTopic> page = new Page<>();
        page.setRecords(Collections.emptyList());
        when(topicMapper.selectPage(any(Page.class), any())).thenReturn(page);
        Page<CommunityTopic> result = discoveryService.hotTopics(20);
        assertNotNull(result);
    }

    @Test
    @DisplayName("用户主页：返回用户帖子")
    void testUserProfile() {
        Page<CommunityPost> page = new Page<>();
        CommunityPost post = new CommunityPost();
        post.setId(100L);
        post.setUserId(1L);
        page.setRecords(List.of(post));
        page.setTotal(1);
        when(postMapper.selectPage(any(Page.class), any())).thenReturn(page);

        PageResponse<CommunityPost> result = discoveryService.userProfile(1L, new PageRequest(1, 20));
        assertNotNull(result);
        assertEquals(1, result.getTotal());
    }

    @Test
    @DisplayName("搜索帖子：关键词为空返回空")
    void testSearchPosts_EmptyKeyword() {
        PageResponse<CommunityPost> result = discoveryService.searchPosts(null, new PageRequest(1, 20));
        assertNotNull(result);
        assertEquals(0, result.getTotal());

        result = discoveryService.searchPosts("", new PageRequest(1, 20));
        assertNotNull(result);
        assertEquals(0, result.getTotal());
    }

    @Test
    @DisplayName("搜索帖子：有关键词返回结果")
    void testSearchPosts_WithKeyword() {
        Page<CommunityPost> page = new Page<>();
        CommunityPost post = new CommunityPost();
        post.setId(100L);
        post.setTitle("测试帖子");
        page.setRecords(List.of(post));
        page.setTotal(1);
        when(postMapper.selectPage(any(Page.class), any())).thenReturn(page);

        PageResponse<CommunityPost> result = discoveryService.searchPosts("测试", new PageRequest(1, 20));
        assertNotNull(result);
        assertEquals(1, result.getTotal());
    }
}
