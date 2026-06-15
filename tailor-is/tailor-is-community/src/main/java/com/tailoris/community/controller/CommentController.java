package com.tailoris.community.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.tailoris.common.dto.PageRequest;
import com.tailoris.common.dto.PageResponse;
import com.tailoris.common.exception.BusinessException;
import com.tailoris.common.result.Result;
import com.tailoris.community.dto.CommentRequest;
import com.tailoris.community.entity.CommunityComment;
import com.tailoris.community.service.CommunityCommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@SaCheckLogin
@Tag(name = "评论管理", description = "评论发布、删除、查询等接口")
@RestController
@RequestMapping("/api/community/comment")
@RequiredArgsConstructor
public class CommentController {

    private final CommunityCommentService communityCommentService;

    @Operation(summary = "发表评论", description = "为帖子发表评论或回复")
    @PostMapping("/create")
    public Result<CommunityComment> createComment(@Valid @RequestBody CommentRequest request) {
        if (!StpUtil.isLogin()) {
            throw new BusinessException("用户未登录");
        }
        Long userId = StpUtil.getLoginIdAsLong();
        CommunityComment comment = communityCommentService.createComment(userId, request);
        return Result.success(comment);
    }

    @Operation(summary = "帖子评论列表", description = "分页查询帖子的评论列表")
    @GetMapping("/list/{postId}")
    public Result<PageResponse<CommunityComment>> listComments(@PathVariable Long postId, PageRequest pageRequest) {
        return Result.success(communityCommentService.listComments(postId, pageRequest));
    }

    @Operation(summary = "删除评论", description = "删除自己的评论")
    @DeleteMapping("/delete/{commentId}")
    public Result<Void> deleteComment(@PathVariable Long commentId) {
        if (!StpUtil.isLogin()) {
            throw new BusinessException("用户未登录");
        }
        Long userId = StpUtil.getLoginIdAsLong();
        communityCommentService.deleteComment(userId, commentId);
        return Result.success();
    }

    @Operation(summary = "我的评论列表", description = "分页查询自己的评论")
    @GetMapping("/my")
    public Result<PageResponse<CommunityComment>> listMyComments(PageRequest pageRequest) {
        if (!StpUtil.isLogin()) {
            throw new BusinessException("用户未登录");
        }
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.success(communityCommentService.listUserComments(userId, pageRequest));
    }
}
