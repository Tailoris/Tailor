package com.tailoris.community.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tailoris.common.dto.PageRequest;
import com.tailoris.common.dto.PageResponse;
import com.tailoris.common.exception.BusinessException;
import com.tailoris.common.util.SnowflakeIdGenerator;
import com.tailoris.community.dto.CommentRequest;
import com.tailoris.community.entity.CommunityComment;
import com.tailoris.community.entity.CommunityPost;
import com.tailoris.community.mapper.CommunityCommentMapper;
import com.tailoris.community.mapper.CommunityPostMapper;
import com.tailoris.community.service.CommunityCommentService;
import com.tailoris.community.service.SensitiveWordFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 社区评论 Service 实现 - 增强版
 * 任务编号: COM-001 配套 - 评论增强
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommunityCommentServiceImpl implements CommunityCommentService {

    private final CommunityCommentMapper communityCommentMapper;
    private final CommunityPostMapper communityPostMapper;
    private final SensitiveWordFilter sensitiveWordFilter;

    @Override
    @Transactional
    public CommunityComment createComment(Long userId, CommentRequest request) {
        CommunityPost post = communityPostMapper.selectById(request.getPostId());
        if (post == null) {
            throw new BusinessException("帖子不存在");
        }
        if (post.getStatus() != null && post.getStatus() == 3) {
            throw new BusinessException("帖子已被删除");
        }
        // 敏感词过滤
        if (sensitiveWordFilter.containsSensitive(request.getContent())) {
            throw new BusinessException("评论包含敏感词");
        }

        CommunityComment comment = new CommunityComment();
        comment.setId(SnowflakeIdGenerator.getInstance().nextId());
        comment.setPostId(request.getPostId());
        comment.setUserId(userId);
        comment.setParentId(request.getParentId() == null ? 0L : request.getParentId());
        comment.setReplyToUserId(request.getReplyToUserId());
        comment.setContent(sensitiveWordFilter.replace(request.getContent()));
        comment.setImages(request.getImages());
        comment.setLikeCount(0);
        comment.setReplyCount(0);
        comment.setStatus(1);
        comment.setAuditStatus(1); // 评论默认通过（机审）

        // 计算楼层（一级评论）
        if (comment.getParentId() == 0L) {
            LambdaQueryWrapper<CommunityComment> countWrapper = new LambdaQueryWrapper<>();
            countWrapper.eq(CommunityComment::getPostId, request.getPostId());
            countWrapper.eq(CommunityComment::getParentId, 0L);
            Long floor = communityCommentMapper.selectCount(countWrapper) + 1;
            comment.setFloor(floor.intValue());
        } else {
            // 二级评论：批量更新父评论的回复数 (M-001)
            communityCommentMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<CommunityComment>()
                    .eq(CommunityComment::getId, comment.getParentId())
                    .setSql("reply_count = reply_count + 1"));
        }

        communityCommentMapper.insert(comment);
        post.setCommentCount((post.getCommentCount() == null ? 0 : post.getCommentCount()) + 1);
        communityPostMapper.updateById(post);
        log.info("创建评论: user={}, post={}, parent={}", userId, request.getPostId(), comment.getParentId());
        return comment;
    }

    @Override
    public PageResponse<CommunityComment> listComments(Long postId, PageRequest pageRequest) {
        Page<CommunityComment> page = new Page<>(pageRequest.getPageNum(), pageRequest.getPageSize());
        LambdaQueryWrapper<CommunityComment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CommunityComment::getPostId, postId)
                .eq(CommunityComment::getStatus, 1)
                .eq(CommunityComment::getParentId, 0L)
                .orderByAsc(CommunityComment::getFloor);
        Page<CommunityComment> result = communityCommentMapper.selectPage(page, wrapper);
        return new PageResponse<>(result.getRecords(), result.getTotal(),
                pageRequest.getPageNum(), pageRequest.getPageSize());
    }

    @Override
    public List<CommunityComment> listReplies(Long parentId) {
        LambdaQueryWrapper<CommunityComment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CommunityComment::getParentId, parentId)
                .eq(CommunityComment::getStatus, 1)
                .orderByAsc(CommunityComment::getCreateTime);
        return communityCommentMapper.selectList(wrapper);
    }

    @Override
    @Transactional
    public void deleteComment(Long userId, Long commentId) {
        CommunityComment comment = communityCommentMapper.selectById(commentId);
        if (comment == null) {
            throw new BusinessException("评论不存在");
        }
        if (!comment.getUserId().equals(userId)) {
            throw new BusinessException("无权限");
        }
        comment.setStatus(3);
        communityCommentMapper.updateById(comment);

        // 父评论回复数减1
        if (comment.getParentId() != null && comment.getParentId() != 0L) {
            // M-001 修复: 使用 SQL 原子更新，避免先查后改的 N+1 竞态
            communityCommentMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<CommunityComment>()
                    .eq(CommunityComment::getId, comment.getParentId())
                    .gt(CommunityComment::getReplyCount, 0)
                    .setSql("reply_count = reply_count - 1"));
        }

        // 帖子评论数减1
        // M-001 修复: 使用 SQL 原子更新，避免先查后改的 N+1 竞态
        communityPostMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<CommunityPost>()
                .eq(CommunityPost::getId, comment.getPostId())
                .gt(CommunityPost::getCommentCount, 0)
                .setSql("comment_count = comment_count - 1"));
    }

    @Override
    public PageResponse<CommunityComment> listUserComments(Long userId, PageRequest pageRequest) {
        Page<CommunityComment> page = new Page<>(pageRequest.getPageNum(), pageRequest.getPageSize());
        LambdaQueryWrapper<CommunityComment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CommunityComment::getUserId, userId)
                .ne(CommunityComment::getStatus, 3)
                .orderByDesc(CommunityComment::getCreateTime);
        Page<CommunityComment> result = communityCommentMapper.selectPage(page, wrapper);
        return new PageResponse<>(result.getRecords(), result.getTotal(),
                pageRequest.getPageNum(), pageRequest.getPageSize());
    }
}
