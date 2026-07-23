package com.tailoris.community.controller;

import com.tailoris.common.dto.PageRequest;
import com.tailoris.common.dto.PageResponse;
import com.tailoris.common.result.Result;
import com.tailoris.community.entity.CommunityPost;
import com.tailoris.community.entity.CommunityTopic;
import com.tailoris.community.service.CommunityDiscoveryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 社区发现与推荐 Controller
 * 任务编号: COM-005
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/community/discover")
@RequiredArgsConstructor
@Tag(name = "社区发现", description = "COM-005 发现页与推荐流")
public class CommunityDiscoveryController {

    private final CommunityDiscoveryService discoveryService;

    @Operation(summary = "热门帖子", description = "获取热门帖子列表")
    @GetMapping("/hot")
    public Result<PageResponse<CommunityPost>> hot(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return Result.success(discoveryService.hotPosts(new PageRequest(pageNum, pageSize)));
    }

    @Operation(summary = "最新帖子", description = "获取最新帖子列表")
    @GetMapping("/latest")
    public Result<PageResponse<CommunityPost>> latest(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return Result.success(discoveryService.latestPosts(new PageRequest(pageNum, pageSize)));
    }

    @Operation(summary = "关注动态", description = "获取关注用户的帖子动态")
    @GetMapping("/following")
    public Result<PageResponse<CommunityPost>> following(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return Result.success(discoveryService.followingFeed(userId, new PageRequest(pageNum, pageSize)));
    }

    @Operation(summary = "话题帖子", description = "根据话题ID获取帖子列表")
    @GetMapping("/topic/{topicId}")
    public Result<PageResponse<CommunityPost>> byTopic(
            @PathVariable Long topicId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return Result.success(discoveryService.postsByTopic(topicId, new PageRequest(pageNum, pageSize)));
    }

    @Operation(summary = "推荐帖子", description = "获取个性化推荐帖子流")
    @GetMapping("/recommend")
    public Result<PageResponse<CommunityPost>> recommend(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return Result.success(discoveryService.recommendFeed(userId, new PageRequest(pageNum, pageSize)));
    }

    @Operation(summary = "热门话题", description = "获取热门话题列表")
    @GetMapping("/topics/hot")
    public Result<Object> hotTopics(@RequestParam(defaultValue = "20") int limit) {
        return Result.success(discoveryService.hotTopics(limit));
    }

    @Operation(summary = "用户主页", description = "获取指定用户的帖子列表")
    @GetMapping("/user/{userId}")
    public Result<PageResponse<CommunityPost>> userProfile(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return Result.success(discoveryService.userProfile(userId, new PageRequest(pageNum, pageSize)));
    }

    @Operation(summary = "搜索帖子", description = "根据关键词搜索帖子")
    @GetMapping("/search")
    public Result<PageResponse<CommunityPost>> search(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return Result.success(discoveryService.searchPosts(keyword, new PageRequest(pageNum, pageSize)));
    }
}
