package com.tailoris.community.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tailoris.common.dto.PageRequest;
import com.tailoris.common.dto.PageResponse;
import com.tailoris.community.entity.CommunityPost;
import com.tailoris.community.entity.CommunityTopic;

/**
 * 社区发现与推荐 Service
 * 任务编号: COM-005
 */
public interface CommunityDiscoveryService {

    /**
     * 热门帖子（按点赞+评论+收藏综合排序）
     */
    PageResponse<CommunityPost> hotPosts(PageRequest pageRequest);

    /**
     * 最新帖子流
     */
    PageResponse<CommunityPost> latestPosts(PageRequest pageRequest);

    /**
     * 关注流（用户关注的人的帖子）
     */
    PageResponse<CommunityPost> followingFeed(Long userId, PageRequest pageRequest);

    /**
     * 话题下的帖子
     */
    PageResponse<CommunityPost> postsByTopic(Long topicId, PageRequest pageRequest);

    /**
     * 推荐流（基于用户兴趣 + 热门）
     */
    PageResponse<CommunityPost> recommendFeed(Long userId, PageRequest pageRequest);

    /**
     * 热门话题
     */
    Page<CommunityTopic> hotTopics(int limit);

    /**
     * 用户主页（个人帖子流）
     */
    PageResponse<CommunityPost> userProfile(Long userId, PageRequest pageRequest);

    /**
     * 搜索帖子
     */
    PageResponse<CommunityPost> searchPosts(String keyword, PageRequest pageRequest);
}
