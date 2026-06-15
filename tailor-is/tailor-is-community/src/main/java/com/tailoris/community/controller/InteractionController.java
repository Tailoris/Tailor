package com.tailoris.community.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.tailoris.common.exception.BusinessException;
import com.tailoris.common.result.Result;
import com.tailoris.community.dto.LikeRequest;
import com.tailoris.community.service.CommunityInteractionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@SaCheckLogin
@Tag(name = "互动管理", description = "点赞、收藏、关注等互动接口")
@RestController
@RequestMapping("/api/community/interaction")
@RequiredArgsConstructor
public class InteractionController {

    private final CommunityInteractionService communityInteractionService;

    @Operation(summary = "点赞", description = "为帖子或评论点赞")
    @PostMapping("/like")
    public Result<Void> like(@Valid @RequestBody LikeRequest request) {
        if (!StpUtil.isLogin()) {
            throw new BusinessException("用户未登录");
        }
        Long userId = StpUtil.getLoginIdAsLong();
        communityInteractionService.like(userId, request.getTargetType(), request.getTargetId());
        return Result.success();
    }

    @Operation(summary = "取消点赞", description = "取消帖子或评论的点赞")
    @DeleteMapping("/unlike")
    public Result<Void> unlike(@Valid @RequestBody LikeRequest request) {
        if (!StpUtil.isLogin()) {
            throw new BusinessException("用户未登录");
        }
        Long userId = StpUtil.getLoginIdAsLong();
        communityInteractionService.unlike(userId, request.getTargetType(), request.getTargetId());
        return Result.success();
    }

    @Operation(summary = "收藏帖子", description = "收藏指定帖子")
    @PostMapping("/favorite")
    public Result<Void> favorite(@RequestParam Long postId) {
        if (!StpUtil.isLogin()) {
            throw new BusinessException("用户未登录");
        }
        Long userId = StpUtil.getLoginIdAsLong();
        communityInteractionService.favorite(userId, postId);
        return Result.success();
    }

    @Operation(summary = "取消收藏", description = "取消收藏指定帖子")
    @DeleteMapping("/unfavorite")
    public Result<Void> unfavorite(@RequestParam Long postId) {
        if (!StpUtil.isLogin()) {
            throw new BusinessException("用户未登录");
        }
        Long userId = StpUtil.getLoginIdAsLong();
        communityInteractionService.unfavorite(userId, postId);
        return Result.success();
    }

    @Operation(summary = "关注用户", description = "关注指定用户")
    @PostMapping("/follow")
    public Result<Void> follow(@RequestParam Long followUserId) {
        if (!StpUtil.isLogin()) {
            throw new BusinessException("用户未登录");
        }
        Long userId = StpUtil.getLoginIdAsLong();
        communityInteractionService.follow(userId, followUserId);
        return Result.success();
    }

    @Operation(summary = "取消关注", description = "取消关注指定用户")
    @DeleteMapping("/unfollow")
    public Result<Void> unfollow(@RequestParam Long followUserId) {
        if (!StpUtil.isLogin()) {
            throw new BusinessException("用户未登录");
        }
        Long userId = StpUtil.getLoginIdAsLong();
        communityInteractionService.unfollow(userId, followUserId);
        return Result.success();
    }

    @Operation(summary = "是否已点赞", description = "检查当前用户是否已点赞")
    @GetMapping("/is-liked")
    public Result<Boolean> isLiked(@RequestParam Integer targetType, @RequestParam Long targetId) {
        if (!StpUtil.isLogin()) {
            throw new BusinessException("用户未登录");
        }
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.success(communityInteractionService.isLiked(userId, targetType, targetId));
    }

    @Operation(summary = "是否已收藏", description = "检查当前用户是否已收藏")
    @GetMapping("/is-favorited")
    public Result<Boolean> isFavorited(@RequestParam Long postId) {
        if (!StpUtil.isLogin()) {
            throw new BusinessException("用户未登录");
        }
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.success(communityInteractionService.isFavorite(userId, postId));
    }

    @Operation(summary = "是否已关注", description = "检查当前用户是否已关注")
    @GetMapping("/is-followed")
    public Result<Boolean> isFollowed(@RequestParam Long followUserId) {
        if (!StpUtil.isLogin()) {
            throw new BusinessException("用户未登录");
        }
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.success(communityInteractionService.isFollowed(userId, followUserId));
    }

    @Operation(summary = "我的收藏数", description = "获取当前用户的收藏总数")
    @GetMapping("/favorite-count")
    public Result<Long> getFavoriteCount() {
        if (!StpUtil.isLogin()) {
            throw new BusinessException("用户未登录");
        }
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.success(communityInteractionService.getFavoriteCount(userId));
    }

    @Operation(summary = "我的粉丝数", description = "获取当前用户的粉丝总数")
    @GetMapping("/follower-count")
    public Result<Long> getFollowerCount() {
        if (!StpUtil.isLogin()) {
            throw new BusinessException("用户未登录");
        }
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.success(communityInteractionService.getFollowerCount(userId));
    }

    @Operation(summary = "我的关注数", description = "获取当前用户的关注总数")
    @GetMapping("/following-count")
    public Result<Long> getFollowingCount() {
        if (!StpUtil.isLogin()) {
            throw new BusinessException("用户未登录");
        }
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.success(communityInteractionService.getFollowingCount(userId));
    }
}
