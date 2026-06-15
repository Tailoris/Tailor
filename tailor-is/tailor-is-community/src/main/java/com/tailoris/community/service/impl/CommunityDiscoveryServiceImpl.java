package com.tailoris.community.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tailoris.common.dto.PageRequest;
import com.tailoris.common.dto.PageResponse;
import com.tailoris.common.util.SnowflakeIdGenerator;
import com.tailoris.community.entity.CommunityFollow;
import com.tailoris.community.entity.CommunityPost;
import com.tailoris.community.entity.CommunityTopic;
import com.tailoris.community.mapper.CommunityFollowMapper;
import com.tailoris.community.mapper.CommunityPostMapper;
import com.tailoris.community.mapper.CommunityTopicMapper;
import com.tailoris.community.mapper.CommunityPostTopicMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 社区发现与推荐 Service 实现
 * 任务编号: COM-005
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommunityDiscoveryServiceImpl implements CommunityDiscoveryService {

    private final CommunityPostMapper postMapper;
    private final CommunityTopicMapper topicMapper;
    private final CommunityPostTopicMapper postTopicMapper;
    private final CommunityFollowMapper followMapper;
    private final StringRedisTemplate stringRedisTemplate;

    private static final String HOT_POSTS_KEY = "community:discover:hot";
    private static final long CACHE_TTL = 300; // 5min

    @Override
    public PageResponse<CommunityPost> hotPosts(PageRequest pageRequest) {
        // 简化：基于 (like_count*2 + comment_count*3 + view_count*0.1) 排序
        Page<CommunityPost> page = new Page<>(pageRequest.getPageNum(), pageRequest.getPageSize());
        LambdaQueryWrapper<CommunityPost> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CommunityPost::getStatus, 2) // 已发布
                .eq(CommunityPost::getAuditStatus, 1) // 审核通过
                .orderByDesc(CommunityPost::getIsEssence)
                .orderByDesc(CommunityPost::getLikeCount)
                .orderByDesc(CommunityPost::getCommentCount)
                .orderByDesc(CommunityPost::getCreateTime);
        Page<CommunityPost> result = postMapper.selectPage(page, wrapper);
        return new PageResponse<>(result.getRecords(), result.getTotal(),
                pageRequest.getPageNum(), pageRequest.getPageSize());
    }

    @Override
    public PageResponse<CommunityPost> latestPosts(PageRequest pageRequest) {
        Page<CommunityPost> page = new Page<>(pageRequest.getPageNum(), pageRequest.getPageSize());
        LambdaQueryWrapper<CommunityPost> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CommunityPost::getStatus, 2)
                .eq(CommunityPost::getAuditStatus, 1)
                .orderByDesc(CommunityPost::getIsTop)
                .orderByDesc(CommunityPost::getCreateTime);
        Page<CommunityPost> result = postMapper.selectPage(page, wrapper);
        return new PageResponse<>(result.getRecords(), result.getTotal(),
                pageRequest.getPageNum(), pageRequest.getPageSize());
    }

    @Override
    public PageResponse<CommunityPost> followingFeed(Long userId, PageRequest pageRequest) {
        if (userId == null) {
            return latestPosts(pageRequest);
        }
        // 查询我关注的人
        List<CommunityFollow> follows = followMapper.selectFollowingByUser(userId);
        if (follows.isEmpty()) {
            return new PageResponse<>(List.of(), 0L, pageRequest.getPageNum(), pageRequest.getPageSize());
        }
        List<Long> userIds = follows.stream()
                .map(CommunityFollow::getTargetUserId)
                .filter(uid -> uid != null)
                .toList();
        if (userIds.isEmpty()) {
            return new PageResponse<>(List.of(), 0L, pageRequest.getPageNum(), pageRequest.getPageSize());
        }
        Page<CommunityPost> page = new Page<>(pageRequest.getPageNum(), pageRequest.getPageSize());
        LambdaQueryWrapper<CommunityPost> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(CommunityPost::getUserId, userIds)
                .eq(CommunityPost::getStatus, 2)
                .eq(CommunityPost::getAuditStatus, 1)
                .orderByDesc(CommunityPost::getCreateTime);
        Page<CommunityPost> result = postMapper.selectPage(page, wrapper);
        return new PageResponse<>(result.getRecords(), result.getTotal(),
                pageRequest.getPageNum(), pageRequest.getPageSize());
    }

    @Override
    public PageResponse<CommunityPost> postsByTopic(Long topicId, PageRequest pageRequest) {
        if (topicId == null) {
            return new PageResponse<>(List.of(), 0L, pageRequest.getPageNum(), pageRequest.getPageSize());
        }
        // 查询话题下的帖子ID
        List<Long> postIds = postTopicMapper.selectByTopicId(topicId).stream()
                .map(pt -> pt.getPostId())
                .filter(id -> id != null)
                .toList();
        if (postIds.isEmpty()) {
            return new PageResponse<>(List.of(), 0L, pageRequest.getPageNum(), pageRequest.getPageSize());
        }
        Page<CommunityPost> page = new Page<>(pageRequest.getPageNum(), pageRequest.getPageSize());
        LambdaQueryWrapper<CommunityPost> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(CommunityPost::getId, postIds)
                .eq(CommunityPost::getStatus, 2)
                .eq(CommunityPost::getAuditStatus, 1)
                .orderByDesc(CommunityPost::getCreateTime);
        Page<CommunityPost> result = postMapper.selectPage(page, wrapper);
        return new PageResponse<>(result.getRecords(), result.getTotal(),
                pageRequest.getPageNum(), pageRequest.getPageSize());
    }

    @Override
    public PageResponse<CommunityPost> recommendFeed(Long userId, PageRequest pageRequest) {
        // 简化实现：综合热门 + 最新
        Page<CommunityPost> page = new Page<>(pageRequest.getPageNum(), pageRequest.getPageSize());
        LambdaQueryWrapper<CommunityPost> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CommunityPost::getStatus, 2)
                .eq(CommunityPost::getAuditStatus, 1)
                .orderByDesc(CommunityPost::getIsRecommend)
                .orderByDesc(CommunityPost::getIsEssence)
                .orderByDesc(CommunityPost::getLikeCount)
                .orderByDesc(CommunityPost::getCreateTime);
        Page<CommunityPost> result = postMapper.selectPage(page, wrapper);
        return new PageResponse<>(result.getRecords(), result.getTotal(),
                pageRequest.getPageNum(), pageRequest.getPageSize());
    }

    @Override
    public Page<CommunityTopic> hotTopics(int limit) {
        Page<CommunityTopic> page = new Page<>(1, limit);
        LambdaQueryWrapper<CommunityTopic> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CommunityTopic::getStatus, 1)
                .orderByDesc(CommunityTopic::getIsHot)
                .orderByDesc(CommunityTopic::getPostCount);
        return topicMapper.selectPage(page, wrapper);
    }

    @Override
    public PageResponse<CommunityPost> userProfile(Long userId, PageRequest pageRequest) {
        Page<CommunityPost> page = new Page<>(pageRequest.getPageNum(), pageRequest.getPageSize());
        LambdaQueryWrapper<CommunityPost> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CommunityPost::getUserId, userId)
                .ne(CommunityPost::getStatus, 3)
                .orderByDesc(CommunityPost::getCreateTime);
        Page<CommunityPost> result = postMapper.selectPage(page, wrapper);
        return new PageResponse<>(result.getRecords(), result.getTotal(),
                pageRequest.getPageNum(), pageRequest.getPageSize());
    }

    @Override
    public PageResponse<CommunityPost> searchPosts(String keyword, PageRequest pageRequest) {
        if (keyword == null || keyword.isEmpty()) {
            return new PageResponse<>(List.of(), 0L, pageRequest.getPageNum(), pageRequest.getPageSize());
        }
        Page<CommunityPost> page = new Page<>(pageRequest.getPageNum(), pageRequest.getPageSize());
        LambdaQueryWrapper<CommunityPost> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(CommunityPost::getTitle, keyword)
                .or().like(CommunityPost::getContent, keyword)
                .eq(CommunityPost::getStatus, 2)
                .eq(CommunityPost::getAuditStatus, 1)
                .orderByDesc(CommunityPost::getCreateTime);
        Page<CommunityPost> result = postMapper.selectPage(page, wrapper);
        return new PageResponse<>(result.getRecords(), result.getTotal(),
                pageRequest.getPageNum(), pageRequest.getPageSize());
    }
}
