package com.tailoris.community.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tailoris.common.dto.PageRequest;
import com.tailoris.common.dto.PageResponse;
import com.tailoris.common.exception.BusinessException;
import com.tailoris.community.dto.PostCreateRequest;
import com.tailoris.community.entity.CommunityPost;
import com.tailoris.community.entity.CommunityPostTopic;
import com.tailoris.community.mapper.CommunityPostMapper;
import com.tailoris.community.mapper.CommunityPostTopicMapper;
import com.tailoris.community.mapper.CommunityTopicMapper;
import com.tailoris.community.service.SensitiveWordFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("CommunityPostServiceImpl 单元测试")
@ExtendWith(MockitoExtension.class)
class CommunityPostServiceImplTest {

    @Mock
    private CommunityPostMapper communityPostMapper;
    @Mock
    private CommunityPostTopicMapper postTopicMapper;
    @Mock
    private CommunityTopicMapper topicMapper;
    @Mock
    private SensitiveWordFilter sensitiveWordFilter;
    @Mock
    private StringRedisTemplate stringRedisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private CommunityPostServiceImpl postService;

    // ==================== createPost ====================

    @Test
    @DisplayName("创建帖子 - 正常创建成功")
    void testCreatePost_Success() {
        PostCreateRequest request = new PostCreateRequest();
        request.setTitle("正常标题");
        request.setContent("正常内容");
        request.setType(1);

        when(sensitiveWordFilter.containsSensitive(anyString())).thenReturn(false);
        when(sensitiveWordFilter.replace(anyString())).thenAnswer(inv -> inv.getArgument(0));
        doReturn(1).when(communityPostMapper).insert(any(CommunityPost.class));

        try (MockedStatic<com.tailoris.common.util.SnowflakeIdGenerator> mock = mockStatic(com.tailoris.common.util.SnowflakeIdGenerator.class)) {
            com.tailoris.common.util.SnowflakeIdGenerator gen = mock(com.tailoris.common.util.SnowflakeIdGenerator.class);
            mock.when(com.tailoris.common.util.SnowflakeIdGenerator::getInstance).thenReturn(gen);
            when(gen.nextId()).thenReturn(12345L);

            CommunityPost result = postService.createPost(1L, request);

            assertNotNull(result);
            assertEquals(1L, result.getUserId());
            assertEquals(0, result.getViewCount());
            assertEquals(0, result.getLikeCount());
            verify(communityPostMapper).insert(any(CommunityPost.class));
        }
    }

    @Test
    @DisplayName("创建帖子 - 包含敏感词抛异常")
    void testCreatePost_SensitiveWord() {
        PostCreateRequest request = new PostCreateRequest();
        request.setTitle("违规标题");
        request.setContent("违规内容");

        when(sensitiveWordFilter.containsSensitive(anyString())).thenReturn(true);

        assertThrows(BusinessException.class, () -> postService.createPost(1L, request));
        verify(communityPostMapper, never()).insert(any(CommunityPost.class));
    }

    @Test
    @DisplayName("创建帖子 - 带话题关联")
    void testCreatePost_WithTopics() {
        PostCreateRequest request = new PostCreateRequest();
        request.setTitle("话题帖子");
        request.setContent("内容");
        request.setTopicIds(List.of(100L, 200L));

        when(sensitiveWordFilter.containsSensitive(anyString())).thenReturn(false);
        when(sensitiveWordFilter.replace(anyString())).thenAnswer(inv -> inv.getArgument(0));
        doReturn(1).when(communityPostMapper).insert(any(CommunityPost.class));
        doReturn(1).when(postTopicMapper).insert(any(CommunityPostTopic.class));

        try (MockedStatic<com.tailoris.common.util.SnowflakeIdGenerator> mock = mockStatic(com.tailoris.common.util.SnowflakeIdGenerator.class)) {
            com.tailoris.common.util.SnowflakeIdGenerator gen = mock(com.tailoris.common.util.SnowflakeIdGenerator.class);
            mock.when(com.tailoris.common.util.SnowflakeIdGenerator::getInstance).thenReturn(gen);
            when(gen.nextId()).thenReturn(12345L);

            postService.createPost(1L, request);

            verify(postTopicMapper, times(2)).insert(any(CommunityPostTopic.class));
        }
    }

    // ==================== updatePost ====================

    @Test
    @DisplayName("更新帖子 - 本人帖子更新成功")
    void testUpdatePost_Success() {
        CommunityPost existingPost = new CommunityPost();
        existingPost.setId(10L);
        existingPost.setUserId(1L);
        existingPost.setStatus(0);

        PostCreateRequest request = new PostCreateRequest();
        request.setTitle("更新标题");
        request.setContent("更新内容");

        when(communityPostMapper.selectById(10L)).thenReturn(existingPost);
        when(sensitiveWordFilter.containsSensitive(anyString())).thenReturn(false);
        when(sensitiveWordFilter.replace(anyString())).thenAnswer(inv -> inv.getArgument(0));
        doReturn(1).when(communityPostMapper).updateById(any(CommunityPost.class));
        when(stringRedisTemplate.delete(anyString())).thenReturn(true);

        CommunityPost result = postService.updatePost(1L, 10L, request);

        assertNotNull(result);
        verify(communityPostMapper).updateById(any(CommunityPost.class));
    }

    @Test
    @DisplayName("更新帖子 - 帖子不存在抛异常")
    void testUpdatePost_NotFound() {
        when(communityPostMapper.selectById(99L)).thenReturn(null);
        PostCreateRequest request = new PostCreateRequest();
        request.setTitle("标题");
        request.setContent("内容");
        assertThrows(BusinessException.class, () -> postService.updatePost(1L, 99L, request));
    }

    @Test
    @DisplayName("更新帖子 - 非本人帖子抛异常")
    void testUpdatePost_NotOwner() {
        CommunityPost existingPost = new CommunityPost();
        existingPost.setId(10L);
        existingPost.setUserId(999L);
        when(communityPostMapper.selectById(10L)).thenReturn(existingPost);

        PostCreateRequest request = new PostCreateRequest();
        request.setTitle("标题");
        request.setContent("内容");
        assertThrows(BusinessException.class, () -> postService.updatePost(1L, 10L, request));
    }

    @Test
    @DisplayName("更新帖子 - 已删除帖子不能更新")
    void testUpdatePost_Deleted() {
        CommunityPost existingPost = new CommunityPost();
        existingPost.setId(10L);
        existingPost.setUserId(1L);
        existingPost.setStatus(3);
        when(communityPostMapper.selectById(10L)).thenReturn(existingPost);

        PostCreateRequest request = new PostCreateRequest();
        request.setTitle("标题");
        request.setContent("内容");
        assertThrows(BusinessException.class, () -> postService.updatePost(1L, 10L, request));
    }

    @Test
    @DisplayName("更新帖子 - 已发布帖子不能修改")
    void testUpdatePost_AlreadyPublished() {
        CommunityPost existingPost = new CommunityPost();
        existingPost.setId(10L);
        existingPost.setUserId(1L);
        existingPost.setStatus(1);
        when(communityPostMapper.selectById(10L)).thenReturn(existingPost);

        PostCreateRequest request = new PostCreateRequest();
        request.setTitle("标题");
        request.setContent("内容");
        assertThrows(BusinessException.class, () -> postService.updatePost(1L, 10L, request));
    }

    // ==================== deletePost ====================

    @Test
    @DisplayName("删除帖子 - 本人帖子删除成功")
    void testDeletePost_Success() {
        CommunityPost post = new CommunityPost();
        post.setId(10L);
        post.setUserId(1L);
        when(communityPostMapper.selectById(10L)).thenReturn(post);
        doReturn(1).when(communityPostMapper).updateById(any(CommunityPost.class));
        when(stringRedisTemplate.delete(anyString())).thenReturn(true);

        postService.deletePost(1L, 10L);

        verify(communityPostMapper).updateById(any(CommunityPost.class));
    }

    @Test
    @DisplayName("删除帖子 - 非本人帖子抛异常")
    void testDeletePost_NotOwner() {
        CommunityPost post = new CommunityPost();
        post.setId(10L);
        post.setUserId(999L);
        when(communityPostMapper.selectById(10L)).thenReturn(post);

        assertThrows(BusinessException.class, () -> postService.deletePost(1L, 10L));
    }

    // ==================== getPostDetail ====================

    @Test
    @DisplayName("获取帖子详情 - 成功")
    void testGetPostDetail_Success() {
        CommunityPost post = new CommunityPost();
        post.setId(10L);
        post.setStatus(2);
        when(communityPostMapper.selectById(10L)).thenReturn(post);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString())).thenReturn(1L);
        when(stringRedisTemplate.expire(anyString(), anyLong(), any())).thenReturn(true);

        CommunityPost result = postService.getPostDetail(10L);

        assertNotNull(result);
        assertEquals(10L, result.getId());
    }

    @Test
    @DisplayName("获取帖子详情 - 帖子不存在抛异常")
    void testGetPostDetail_NotFound() {
        when(communityPostMapper.selectById(99L)).thenReturn(null);
        assertThrows(BusinessException.class, () -> postService.getPostDetail(99L));
    }

    @Test
    @DisplayName("获取帖子详情 - 已删除帖子抛异常")
    void testGetPostDetail_Deleted() {
        CommunityPost post = new CommunityPost();
        post.setId(10L);
        post.setStatus(3);
        when(communityPostMapper.selectById(10L)).thenReturn(post);
        assertThrows(BusinessException.class, () -> postService.getPostDetail(10L));
    }

    // ==================== listPosts ====================

    @Test
    @DisplayName("帖子列表 - 分页查询")
    void testListPosts() {
        Page<CommunityPost> page = new Page<>();
        page.setRecords(Collections.emptyList());
        page.setTotal(0);
        when(communityPostMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);

        PageResponse<CommunityPost> result = postService.listPosts(new PageRequest(1, 20), null, null, null);

        assertNotNull(result);
        assertEquals(0, result.getTotal());
    }

    @Test
    @DisplayName("帖子列表 - 按分类过滤")
    void testListPosts_ByCategory() {
        Page<CommunityPost> page = new Page<>();
        page.setRecords(Collections.emptyList());
        page.setTotal(0);
        when(communityPostMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);

        PageResponse<CommunityPost> result = postService.listPosts(new PageRequest(1, 20), 5L, 1, null);

        assertNotNull(result);
    }

    // ==================== listUserPosts ====================

    @Test
    @DisplayName("用户帖子列表")
    void testListUserPosts() {
        Page<CommunityPost> page = new Page<>();
        page.setRecords(Collections.emptyList());
        page.setTotal(0);
        when(communityPostMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);

        PageResponse<CommunityPost> result = postService.listUserPosts(1L, new PageRequest(1, 20));

        assertNotNull(result);
    }

    // ==================== auditPost ====================

    @Test
    @DisplayName("审核帖子 - 通过")
    void testAuditPost_Approve() {
        CommunityPost post = new CommunityPost();
        post.setId(10L);
        when(communityPostMapper.selectById(10L)).thenReturn(post);
        doReturn(1).when(communityPostMapper).updateById(any(CommunityPost.class));
        when(stringRedisTemplate.delete(anyString())).thenReturn(true);

        postService.auditPost(10L, 1, 99L, "审核通过");
        verify(communityPostMapper).updateById(any(CommunityPost.class));
    }

    @Test
    @DisplayName("审核帖子 - 拒绝")
    void testAuditPost_Reject() {
        CommunityPost post = new CommunityPost();
        post.setId(10L);
        when(communityPostMapper.selectById(10L)).thenReturn(post);
        doReturn(1).when(communityPostMapper).updateById(any(CommunityPost.class));
        when(stringRedisTemplate.delete(anyString())).thenReturn(true);

        postService.auditPost(10L, 2, 99L, "内容不合规");

        verify(communityPostMapper).updateById(any(CommunityPost.class));
    }

    @Test
    @DisplayName("审核帖子 - 帖子不存在抛异常")
    void testAuditPost_NotFound() {
        when(communityPostMapper.selectById(99L)).thenReturn(null);
        assertThrows(BusinessException.class, () -> postService.auditPost(99L, 1, 99L, "ok"));
    }

    // ==================== setTop / setEssence ====================

    @Test
    @DisplayName("置顶帖子")
    void testSetTop() {
        CommunityPost post = new CommunityPost();
        post.setId(10L);
        when(communityPostMapper.selectById(10L)).thenReturn(post);
        doReturn(1).when(communityPostMapper).updateById(any(CommunityPost.class));

        postService.setTop(10L, 1);

        verify(communityPostMapper).updateById(any(CommunityPost.class));
    }

    @Test
    @DisplayName("置顶帖子 - 不存在抛异常")
    void testSetTop_NotFound() {
        when(communityPostMapper.selectById(99L)).thenReturn(null);
        assertThrows(BusinessException.class, () -> postService.setTop(99L, 1));
    }

    @Test
    @DisplayName("加精帖子")
    void testSetEssence() {
        CommunityPost post = new CommunityPost();
        post.setId(10L);
        when(communityPostMapper.selectById(10L)).thenReturn(post);
        doReturn(1).when(communityPostMapper).updateById(any(CommunityPost.class));

        postService.setEssence(10L, 1);

        verify(communityPostMapper).updateById(any(CommunityPost.class));
    }

    @Test
    @DisplayName("加精帖子 - 不存在抛异常")
    void testSetEssence_NotFound() {
        when(communityPostMapper.selectById(99L)).thenReturn(null);
        assertThrows(BusinessException.class, () -> postService.setEssence(99L, 1));
    }
}
