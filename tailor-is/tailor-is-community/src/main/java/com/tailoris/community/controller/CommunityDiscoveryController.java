package com.tailoris.community.controller;

import com.tailoris.common.dto.PageRequest;
import com.tailoris.common.dto.PageResponse;
import com.tailoris.common.result.Result;
import com.tailoris.community.entity.CommunityPost;
import com.tailoris.community.entity.CommunityTopic;
import com.tailoris.community.service.CommunityDiscoveryService;
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
@RequestMapping("/api/community/discover")
@RequiredArgsConstructor
@Tag(name = "社区发现", description = "COM-005 发现页与推荐流")
public class CommunityDiscoveryController {

    private final CommunityDiscoveryService discoveryService;

    @GetMapping("/hot")
    public Result<PageResponse<CommunityPost>> hot(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return Result.success(discoveryService.hotPosts(new PageRequest(pageNum, pageSize)));
    }

    @GetMapping("/latest")
    public Result<PageResponse<CommunityPost>> latest(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return Result.success(discoveryService.latestPosts(new PageRequest(pageNum, pageSize)));
    }

    @GetMapping("/following")
    public Result<PageResponse<CommunityPost>> following(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return Result.success(discoveryService.followingFeed(userId, new PageRequest(pageNum, pageSize)));
    }

    @GetMapping("/topic/{topicId}")
    public Result<PageResponse<CommunityPost>> byTopic(
            @PathVariable Long topicId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return Result.success(discoveryService.postsByTopic(topicId, new PageRequest(pageNum, pageSize)));
    }

    @GetMapping("/recommend")
    public Result<PageResponse<CommunityPost>> recommend(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return Result.success(discoveryService.recommendFeed(userId, new PageRequest(pageNum, pageSize)));
    }

    @GetMapping("/topics/hot")
    public Result<Object> hotTopics(@RequestParam(defaultValue = "20") int limit) {
        return Result.success(discoveryService.hotTopics(limit));
    }

    @GetMapping("/user/{userId}")
    public Result<PageResponse<CommunityPost>> userProfile(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return Result.success(discoveryService.userProfile(userId, new PageRequest(pageNum, pageSize)));
    }

    @GetMapping("/search")
    public Result<PageResponse<CommunityPost>> search(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return Result.success(discoveryService.searchPosts(keyword, new PageRequest(pageNum, pageSize)));
    }
}
