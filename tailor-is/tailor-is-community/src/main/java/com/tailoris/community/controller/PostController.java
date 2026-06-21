package com.tailoris.community.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.stp.StpUtil;
import com.tailoris.common.dto.PageRequest;
import com.tailoris.common.dto.PageResponse;
import com.tailoris.common.exception.BusinessException;
import com.tailoris.common.result.Result;
import com.tailoris.community.dto.PostCreateRequest;
import com.tailoris.community.entity.CommunityPost;
import com.tailoris.community.service.CommunityPostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@SaCheckLogin
@Tag(name = "帖子管理", description = "帖子发布、编辑、删除、查询等接口")
@RestController
@RequestMapping("/api/community/post")
@RequiredArgsConstructor
public class PostController {

    private final CommunityPostService communityPostService;

    @Operation(summary = "发布帖子", description = "发布新帖子")
    @PostMapping("/create")
    public Result<CommunityPost> createPost(@Valid @RequestBody PostCreateRequest request) {
        if (!StpUtil.isLogin()) {
            throw new BusinessException("用户未登录");
        }
        Long userId = StpUtil.getLoginIdAsLong();
        CommunityPost post = communityPostService.createPost(userId, request);
        return Result.success(post);
    }

    @Operation(summary = "编辑帖子", description = "编辑自己的帖子")
    @PutMapping("/update/{postId}")
    public Result<CommunityPost> updatePost(@PathVariable Long postId, @Valid @RequestBody PostCreateRequest request) {
        if (!StpUtil.isLogin()) {
            throw new BusinessException("用户未登录");
        }
        Long userId = StpUtil.getLoginIdAsLong();
        CommunityPost post = communityPostService.updatePost(userId, postId, request);
        return Result.success(post);
    }

    @Operation(summary = "删除帖子", description = "删除自己的帖子")
    @DeleteMapping("/delete/{postId}")
    public Result<Void> deletePost(@PathVariable Long postId) {
        if (!StpUtil.isLogin()) {
            throw new BusinessException("用户未登录");
        }
        Long userId = StpUtil.getLoginIdAsLong();
        communityPostService.deletePost(userId, postId);
        return Result.success();
    }

    @Operation(summary = "帖子详情", description = "查看帖子详情")
    @GetMapping("/detail/{postId}")
    public Result<CommunityPost> getPostDetail(@PathVariable Long postId) {
        return Result.success(communityPostService.getPostDetail(postId));
    }

    @Operation(summary = "帖子列表", description = "分页查询帖子列表")
    @GetMapping("/list")
    public Result<PageResponse<CommunityPost>> listPosts(PageRequest pageRequest, @RequestParam(required = false) Long categoryId, @RequestParam(required = false) Integer type, @RequestParam(required = false) String tag) {
        return Result.success(communityPostService.listPosts(pageRequest, categoryId, type, tag));
    }

    @Operation(summary = "我的帖子列表", description = "分页查询自己的帖子")
    @GetMapping("/my")
    public Result<PageResponse<CommunityPost>> listMyPosts(PageRequest pageRequest) {
        if (!StpUtil.isLogin()) {
            throw new BusinessException("用户未登录");
        }
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.success(communityPostService.listUserPosts(userId, pageRequest));
    }

    @Operation(summary = "置顶/取消置顶", description = "运营：置顶帖子")
    @SaCheckRole("admin")
    @PostMapping("/top/{postId}")
    public Result<Void> setTop(@PathVariable Long postId, @RequestParam Integer isTop) {
        communityPostService.setTop(postId, isTop);
        return Result.success();
    }

    @Operation(summary = "加精/取消加精", description = "运营：加精帖子")
    @SaCheckRole("admin")
    @PostMapping("/essence/{postId}")
    public Result<Void> setEssence(@PathVariable Long postId, @RequestParam Integer isEssence) {
        communityPostService.setEssence(postId, isEssence);
        return Result.success();
    }
}
