package com.tailoris.community.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.tailoris.common.dto.PageRequest;
import com.tailoris.common.dto.PageResponse;
import com.tailoris.common.exception.BusinessException;
import com.tailoris.common.result.Result;
import com.tailoris.community.dto.CommentRequest;
import com.tailoris.community.dto.LikeRequest;
import com.tailoris.community.dto.PostCreateRequest;
import com.tailoris.community.entity.CommunityComment;
import com.tailoris.community.entity.CommunityPost;
import com.tailoris.community.service.CommunityCommentService;
import com.tailoris.community.service.CommunityInteractionService;
import com.tailoris.community.service.CommunityPostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import com.tailoris.common.constant.RoutePortConstant;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "社区公开接口", description = "首页社区展示相关接口，无需登录")
@RestController
@RequestMapping(RoutePortConstant.ROUTE_COMMUNITY_POST)
@RequiredArgsConstructor
public class PublicPostController {

    private final CommunityPostService communityPostService;
    private final CommunityCommentService communityCommentService;
    private final CommunityInteractionService communityInteractionService;

    @Operation(summary = "帖子列表", description = "分页查询帖子列表（公开访问）")
    @GetMapping
    public Result<PageResponse<CommunityPost>> listPosts(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Integer type) {
        return doListPosts(current, size, type);
    }

    @Operation(summary = "帖子列表（兼容路径）", description = "分页查询帖子列表，兼容旧版/list路径")
    @GetMapping("/list")
    public Result<PageResponse<CommunityPost>> listPostsLegacy(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Integer type) {
        return doListPosts(current, size, type);
    }

    private Result<PageResponse<CommunityPost>> doListPosts(Integer current, Integer size, Integer type) {
        PageRequest pageRequest = new PageRequest();
        pageRequest.setPageNum(current);
        pageRequest.setPageSize(size);
        return Result.success(communityPostService.listPosts(pageRequest, null, type, null));
    }

    @Operation(summary = "帖子详情", description = "查看帖子详情（公开访问）")
    @GetMapping("/{id}")
    public Result<CommunityPost> getPostDetail(@PathVariable Long id) {
        return Result.success(communityPostService.getPostDetail(id));
    }

    @SaCheckLogin
    @Operation(summary = "发布帖子", description = "发布新帖子")
    @PostMapping
    public Result<CommunityPost> createPost(@Valid @RequestBody PostCreateRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        CommunityPost post = communityPostService.createPost(userId, request);
        return Result.success(post);
    }

    @SaCheckLogin
    @Operation(summary = "点赞帖子", description = "为帖子点赞")
    @PostMapping("/{id}/like")
    public Result<Void> likePost(@PathVariable Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        communityInteractionService.like(userId, 1, id);
        return Result.success();
    }

    @SaCheckLogin
    @Operation(summary = "获取帖子评论", description = "分页查询帖子的评论列表")
    @GetMapping("/{id}/comments")
    public Result<PageResponse<CommunityComment>> getPostComments(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size) {
        PageRequest pageRequest = new PageRequest();
        pageRequest.setPageNum(current);
        pageRequest.setPageSize(size);
        return Result.success(communityCommentService.listComments(id, pageRequest));
    }

    @SaCheckLogin
    @Operation(summary = "发表评论", description = "为帖子发表评论")
    @PostMapping("/{id}/comments")
    public Result<CommunityComment> commentPost(
            @PathVariable Long id,
            @Valid @RequestBody CommentRequest request) {
        request.setPostId(id);
        Long userId = StpUtil.getLoginIdAsLong();
        CommunityComment comment = communityCommentService.createComment(userId, request);
        return Result.success(comment);
    }
}