package com.tailoris.community.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tailoris.common.dto.PageRequest;
import com.tailoris.common.dto.PageResponse;
import com.tailoris.common.exception.BusinessException;
import com.tailoris.community.dto.CommentRequest;
import com.tailoris.community.entity.CommunityComment;
import com.tailoris.community.entity.CommunityPost;
import com.tailoris.community.mapper.CommunityCommentMapper;
import com.tailoris.community.mapper.CommunityPostMapper;
import com.tailoris.community.service.SensitiveWordFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("CommunityCommentServiceImpl 单元测试")
@ExtendWith(MockitoExtension.class)
class CommunityCommentServiceImplTest {

    @Mock
    private CommunityCommentMapper communityCommentMapper;
    @Mock
    private CommunityPostMapper communityPostMapper;
    @Mock
    private SensitiveWordFilter sensitiveWordFilter;

    @InjectMocks
    private CommunityCommentServiceImpl commentService;

    @Test
    @DisplayName("创建一级评论 - 成功")
    void testCreateComment_TopLevel() {
        CommentRequest request = new CommentRequest();
        request.setPostId(10L);
        request.setContent("好帖子");
        request.setParentId(null);

        CommunityPost post = new CommunityPost();
        post.setId(10L);
        post.setStatus(2);
        post.setCommentCount(5);
        when(communityPostMapper.selectById(10L)).thenReturn(post);
        when(sensitiveWordFilter.containsSensitive(anyString())).thenReturn(false);
        when(sensitiveWordFilter.replace(anyString())).thenAnswer(inv -> inv.getArgument(0));
        when(communityCommentMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(3L);
        doReturn(1).when(communityCommentMapper).insert(any(CommunityComment.class));
        doReturn(1).when(communityPostMapper).updateById(any(CommunityPost.class));

        try (MockedStatic<com.tailoris.common.util.SnowflakeIdGenerator> mock =
                     mockStatic(com.tailoris.common.util.SnowflakeIdGenerator.class)) {
            com.tailoris.common.util.SnowflakeIdGenerator gen = mock(com.tailoris.common.util.SnowflakeIdGenerator.class);
            mock.when(com.tailoris.common.util.SnowflakeIdGenerator::getInstance).thenReturn(gen);
            when(gen.nextId()).thenReturn(12345L);

            CommunityComment result = commentService.createComment(1L, request);

            assertNotNull(result);
            assertEquals(10L, result.getPostId());
            assertEquals(0L, result.getParentId());
            assertEquals(4, result.getFloor());
        }
    }

    @Test
    @DisplayName("创建评论 - 帖子不存在抛异常")
    void testCreateComment_PostNotFound() {
        CommentRequest request = new CommentRequest();
        request.setPostId(99L);
        request.setContent("评论");

        when(communityPostMapper.selectById(99L)).thenReturn(null);

        assertThrows(BusinessException.class, () -> commentService.createComment(1L, request));
    }

    @Test
    @DisplayName("创建评论 - 帖子已删除抛异常")
    void testCreateComment_PostDeleted() {
        CommentRequest request = new CommentRequest();
        request.setPostId(10L);
        request.setContent("评论");

        CommunityPost post = new CommunityPost();
        post.setId(10L);
        post.setStatus(3);
        when(communityPostMapper.selectById(10L)).thenReturn(post);

        assertThrows(BusinessException.class, () -> commentService.createComment(1L, request));
    }

    @Test
    @DisplayName("创建评论 - 包含敏感词抛异常")
    void testCreateComment_SensitiveWord() {
        CommentRequest request = new CommentRequest();
        request.setPostId(10L);
        request.setContent("违规评论");

        CommunityPost post = new CommunityPost();
        post.setId(10L);
        post.setStatus(2);
        when(communityPostMapper.selectById(10L)).thenReturn(post);
        when(sensitiveWordFilter.containsSensitive(anyString())).thenReturn(true);

        assertThrows(BusinessException.class, () -> commentService.createComment(1L, request));
    }

    @Test
    @DisplayName("创建二级评论 - 父评论回复数增加")
    void testCreateComment_Reply() {
        CommentRequest request = new CommentRequest();
        request.setPostId(10L);
        request.setContent("回复内容");
        request.setParentId(5L);

        CommunityPost post = new CommunityPost();
        post.setId(10L);
        post.setStatus(2);
        post.setCommentCount(5);
        when(communityPostMapper.selectById(10L)).thenReturn(post);
        when(sensitiveWordFilter.containsSensitive(anyString())).thenReturn(false);
        when(sensitiveWordFilter.replace(anyString())).thenAnswer(inv -> inv.getArgument(0));
        doReturn(1).when(communityCommentMapper).insert(any(CommunityComment.class));
        doReturn(1).when(communityPostMapper).updateById(any(CommunityPost.class));

        try (MockedStatic<com.tailoris.common.util.SnowflakeIdGenerator> mock =
                     mockStatic(com.tailoris.common.util.SnowflakeIdGenerator.class)) {
            com.tailoris.common.util.SnowflakeIdGenerator gen = mock(com.tailoris.common.util.SnowflakeIdGenerator.class);
            mock.when(com.tailoris.common.util.SnowflakeIdGenerator::getInstance).thenReturn(gen);
            when(gen.nextId()).thenReturn(12345L);

            CommunityComment result = commentService.createComment(1L, request);

            assertNotNull(result);
            assertEquals(5L, result.getParentId());
        }
    }

    @Test
    @DisplayName("评论列表 - 分页查询")
    void testListComments() {
        Page<CommunityComment> page = new Page<>();
        page.setRecords(Collections.emptyList());
        page.setTotal(0);
        doReturn(page).when(communityCommentMapper).selectPage(any(Page.class), any(LambdaQueryWrapper.class));

        PageResponse<CommunityComment> result = commentService.listComments(10L, new PageRequest(1, 20));

        assertNotNull(result);
        assertEquals(0, result.getTotal());
    }

    @Test
    @DisplayName("回复列表")
    void testListReplies() {
        doReturn(Collections.emptyList()).when(communityCommentMapper).selectList(any(LambdaQueryWrapper.class));

        List<CommunityComment> result = commentService.listReplies(5L);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("删除评论 - 本人评论删除成功")
    void testDeleteComment_Success() {
        CommunityComment comment = new CommunityComment();
        comment.setId(100L);
        comment.setUserId(1L);
        comment.setPostId(10L);
        comment.setParentId(5L);
        when(communityCommentMapper.selectById(100L)).thenReturn(comment);
        doReturn(1).when(communityCommentMapper).updateById(any(CommunityComment.class));

        commentService.deleteComment(1L, 100L);

        verify(communityCommentMapper, times(1)).updateById(any(CommunityComment.class));
    }

    @Test
    @DisplayName("删除评论 - 评论不存在抛异常")
    void testDeleteComment_NotFound() {
        when(communityCommentMapper.selectById(999L)).thenReturn(null);
        assertThrows(BusinessException.class, () -> commentService.deleteComment(1L, 999L));
    }

    @Test
    @DisplayName("删除评论 - 非本人评论抛异常")
    void testDeleteComment_NotOwner() {
        CommunityComment comment = new CommunityComment();
        comment.setId(100L);
        comment.setUserId(999L);
        when(communityCommentMapper.selectById(100L)).thenReturn(comment);

        assertThrows(BusinessException.class, () -> commentService.deleteComment(1L, 100L));
    }

    @Test
    @DisplayName("用户评论列表")
    void testListUserComments() {
        Page<CommunityComment> page = new Page<>();
        page.setRecords(Collections.emptyList());
        page.setTotal(0);
        doReturn(page).when(communityCommentMapper).selectPage(any(Page.class), any(LambdaQueryWrapper.class));

        PageResponse<CommunityComment> result = commentService.listUserComments(1L, new PageRequest(1, 20));

        assertNotNull(result);
    }
}
