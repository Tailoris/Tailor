package com.tailoris.community.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tailoris.common.dto.PageRequest;
import com.tailoris.common.dto.PageResponse;
import com.tailoris.common.exception.BusinessException;
import com.tailoris.common.util.SnowflakeIdGenerator;
import com.tailoris.community.dto.PostCreateRequest;
import com.tailoris.community.entity.CommunityPost;
import com.tailoris.community.entity.CommunityPostTopic;
import com.tailoris.community.entity.CommunityTopic;
import com.tailoris.community.mapper.CommunityPostMapper;
import com.tailoris.community.mapper.CommunityPostTopicMapper;
import com.tailoris.community.mapper.CommunityTopicMapper;
import com.tailoris.community.service.CommunityPostService;
import com.tailoris.community.service.SensitiveWordFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 社区帖子 Service 实现 - 增强版
 * 任务编号: COM-001
 * 修复: 浏览数 Redis 预增、敏感词过滤、二级评论支持
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommunityPostServiceImpl implements CommunityPostService {

    private final CommunityPostMapper communityPostMapper;
    private final CommunityPostTopicMapper postTopicMapper;
    private final CommunityTopicMapper topicMapper;
    private final SensitiveWordFilter sensitiveWordFilter;
    private final StringRedisTemplate stringRedisTemplate;

    private static final String POST_CACHE_KEY = "community:post:";
    private static final String VIEW_KEY = "community:post:view:";
    private static final long VIEW_CACHE_TTL = 1; // 1小时
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 50;
    private static final int VIEW_SYNC_THRESHOLD = 100; // 100次同步一次到DB

    @Override
    @Transactional
    public CommunityPost createPost(Long userId, PostCreateRequest request) {
        // 1. 敏感词过滤
        if (sensitiveWordFilter.containsSensitive(request.getTitle() + " " + request.getContent())) {
            throw new BusinessException("内容包含敏感词，请修改后重试");
        }

        CommunityPost post = new CommunityPost();
        post.setId(SnowflakeIdGenerator.getInstance().nextId());
        post.setUserId(userId);
        post.setTitle(sensitiveWordFilter.replace(request.getTitle()));
        post.setContent(sensitiveWordFilter.replace(request.getContent()));
        post.setSummary(request.getSummary());
        post.setImages(request.getImages());
        post.setVideoUrl(request.getVideoUrl());
        post.setType(request.getType() != null ? request.getType() : 1);
        post.setCategoryId(request.getCategoryId());
        post.setTags(request.getTags());
        post.setViewCount(0);
        post.setLikeCount(0);
        post.setCommentCount(0);
        post.setShareCount(0);
        post.setCollectCount(0);
        post.setFavoriteCount(0);
        post.setIsTop(0);
        post.setIsEssence(0);
        post.setIsRecommend(0);
        post.setStatus(0); // 待审核
        post.setAuditStatus(0); // 待审核
        post.setRelatedProductId(request.getRelatedProductId());
        post.setRelatedShopId(request.getRelatedShopId());
        post.setLocation(request.getLocation());
        post.setLongitude(request.getLongitude());
        post.setLatitude(request.getLatitude());
        if (request.getTopicIds() != null && !request.getTopicIds().isEmpty()) {
            post.setTopicIds(request.getTopicIds().stream()
                    .map(String::valueOf).collect(Collectors.joining(",")));
        }
        if (request.getProductIds() != null && !request.getProductIds().isEmpty()) {
            post.setProductIds(request.getProductIds().stream()
                    .map(String::valueOf).collect(Collectors.joining(",")));
        }
        communityPostMapper.insert(post);

        // 关联话题
        if (request.getTopicIds() != null) {
            for (Long topicId : request.getTopicIds()) {
                CommunityPostTopic pt = new CommunityPostTopic();
                pt.setId(SnowflakeIdGenerator.getInstance().nextId());
                pt.setPostId(post.getId());
                pt.setTopicId(topicId);
                postTopicMapper.insert(pt);
                // 更新话题帖子数
                try {
                    topicMapper.incrPostCount(topicId);
                } catch (Exception e) {
                    log.warn("更新话题帖子数失败: {}", e.getMessage());
                }
            }
        }
        log.info("创建帖子: user={}, id={}, title={}", userId, post.getId(), post.getTitle());
        return post;
    }

    @Override
    @Transactional
    public CommunityPost updatePost(Long userId, Long postId, PostCreateRequest request) {
        CommunityPost post = communityPostMapper.selectById(postId);
        if (post == null || !post.getUserId().equals(userId)) {
            throw new BusinessException("帖子不存在或无权限");
        }
        if (post.getStatus() != null && post.getStatus() == 3) {
            throw new BusinessException("帖子已被删除");
        }
        if (post.getStatus() != null && post.getStatus() == 1) {
            throw new BusinessException("已发布帖子不能修改");
        }
        if (sensitiveWordFilter.containsSensitive(request.getTitle() + " " + request.getContent())) {
            throw new BusinessException("内容包含敏感词");
        }
        post.setTitle(sensitiveWordFilter.replace(request.getTitle()));
        post.setContent(sensitiveWordFilter.replace(request.getContent()));
        post.setSummary(request.getSummary());
        post.setImages(request.getImages());
        post.setVideoUrl(request.getVideoUrl());
        post.setType(request.getType() != null ? request.getType() : 1);
        post.setCategoryId(request.getCategoryId());
        post.setTags(request.getTags());
        post.setRelatedProductId(request.getRelatedProductId());
        post.setRelatedShopId(request.getRelatedShopId());
        post.setLocation(request.getLocation());
        communityPostMapper.updateById(post);
        stringRedisTemplate.delete(POST_CACHE_KEY + postId);
        return post;
    }

    @Override
    @Transactional
    public void deletePost(Long userId, Long postId) {
        CommunityPost post = communityPostMapper.selectById(postId);
        if (post == null || !post.getUserId().equals(userId)) {
            throw new BusinessException("帖子不存在或无权限");
        }
        post.setStatus(3);
        communityPostMapper.updateById(post);
        stringRedisTemplate.delete(POST_CACHE_KEY + postId);
    }

    @Override
    public CommunityPost getPostDetail(Long postId) {
        CommunityPost post = communityPostMapper.selectById(postId);
        if (post == null) {
            throw new BusinessException("帖子不存在");
        }
        if (post.getStatus() != null && post.getStatus() == 3) {
            throw new BusinessException("帖子已被删除");
        }
        // 异步累加浏览数（Redis 预增）
        incrViewCountAsync(postId);
        return post;
    }

    @Override
    public PageResponse<CommunityPost> listPosts(PageRequest pageRequest, Long categoryId, Integer type, String tag) {
        int pageSize = Math.min(pageRequest.getPageSize(), MAX_PAGE_SIZE);
        Page<CommunityPost> page = new Page<>(pageRequest.getPageNum(), pageSize);
        LambdaQueryWrapper<CommunityPost> wrapper = new LambdaQueryWrapper<>();
        wrapper.ne(CommunityPost::getStatus, 3)
                .eq(CommunityPost::getAuditStatus, 1);
        if (categoryId != null) {
            wrapper.eq(CommunityPost::getCategoryId, categoryId);
        }
        if (type != null) {
            wrapper.eq(CommunityPost::getType, type);
        }
        wrapper.orderByDesc(CommunityPost::getIsTop);
        wrapper.orderByDesc(CommunityPost::getCreateTime);
        Page<CommunityPost> result = communityPostMapper.selectPage(page, wrapper);
        return new PageResponse<>(result.getRecords(), result.getTotal(),
                pageRequest.getPageNum(), pageSize);
    }

    @Override
    public PageResponse<CommunityPost> listUserPosts(Long userId, PageRequest pageRequest) {
        Page<CommunityPost> page = new Page<>(pageRequest.getPageNum(), pageRequest.getPageSize());
        LambdaQueryWrapper<CommunityPost> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CommunityPost::getUserId, userId)
                .ne(CommunityPost::getStatus, 3)
                .orderByDesc(CommunityPost::getCreateTime);
        Page<CommunityPost> result = communityPostMapper.selectPage(page, wrapper);
        return new PageResponse<>(result.getRecords(), result.getTotal(),
                pageRequest.getPageNum(), pageRequest.getPageSize());
    }

    @Override
    @Transactional
    public void auditPost(Long postId, Integer status, Long auditBy, String auditRemark) {
        CommunityPost post = communityPostMapper.selectById(postId);
        if (post == null) {
            throw new BusinessException("帖子不存在");
        }
        // status: 0=待审核 1=通过 2=拒绝
        if (status == 1) {
            post.setStatus(2); // 已发布
            post.setAuditStatus(1); // 审核通过
        } else if (status == 2) {
            post.setStatus(0); // 隐藏
            post.setAuditStatus(2); // 审核拒绝
        }
        post.setAuditUserId(auditBy);
        post.setAuditRemark(auditRemark);
        post.setAuditTime(LocalDateTime.now());
        communityPostMapper.updateById(post);
        stringRedisTemplate.delete(POST_CACHE_KEY + postId);
    }

    @Override
    @Transactional
    public void setTop(Long postId, Integer isTop) {
        CommunityPost post = communityPostMapper.selectById(postId);
        if (post == null) {
            throw new BusinessException("帖子不存在");
        }
        post.setIsTop(isTop);
        communityPostMapper.updateById(post);
    }

    @Override
    @Transactional
    public void setEssence(Long postId, Integer isEssence) {
        CommunityPost post = communityPostMapper.selectById(postId);
        if (post == null) {
            throw new BusinessException("帖子不存在");
        }
        post.setIsEssence(isEssence);
        communityPostMapper.updateById(post);
    }

    /**
     * 异步累加浏览数
     */
    private void incrViewCountAsync(Long postId) {
        try {
            String key = VIEW_KEY + postId;
            Long count = stringRedisTemplate.opsForValue().increment(key);
            stringRedisTemplate.expire(key, VIEW_CACHE_TTL, TimeUnit.HOURS);
            if (count != null && count % VIEW_SYNC_THRESHOLD == 0) {
                // BE-M-26: 使用参数化 Mapper 方法，避免 SQL 字符串拼接
                communityPostMapper.incrementViewCount(postId, count);
                stringRedisTemplate.delete(key);
            }
        } catch (Exception e) {
            log.warn("异步累加浏览数失败: postId={}", postId, e);
        }
    }
}
