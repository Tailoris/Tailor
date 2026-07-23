package com.tailoris.design.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.tailoris.common.dto.PageResponse;
import com.tailoris.common.result.Result;
import com.tailoris.common.constant.RoutePortConstant;
import com.tailoris.design.dto.CommentRequest;
import com.tailoris.design.entity.DesignComment;
import com.tailoris.design.service.DesignCommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(RoutePortConstant.ROUTE_DESIGN + "/comments")
@RequiredArgsConstructor
public class DesignCommentController {

    private final DesignCommentService designCommentService;

    @Operation(summary = "添加评论", description = "添加设计评论")
    @PostMapping
    public Result<DesignComment> addComment(@Valid @RequestBody CommentRequest request) {
        DesignComment comment = new DesignComment();
        comment.setDesignId(request.getDesignId());
        comment.setContent(request.getContent());
        comment.setParentId(request.getParentId() != null ? request.getParentId() : 0L);
        comment.setUserId(1L);
        
        DesignComment created = designCommentService.addComment(comment);
        return Result.success(created);
    }

    @Operation(summary = "删除评论", description = "删除指定评论")
    @DeleteMapping("/{id}")
    public Result<Void> deleteComment(@PathVariable Long id) {
        designCommentService.deleteComment(id);
        return Result.success();
    }

    @Operation(summary = "设计评论列表", description = "获取指定设计的评论列表")
    @GetMapping("/design/{designId}")
    public Result<PageResponse<DesignComment>> getCommentsByDesignId(
            @PathVariable Long designId,
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size) {
        IPage<DesignComment> page = designCommentService.getCommentsByDesignId(designId, current, size);
        return Result.success(new PageResponse<>(page.getRecords(), page.getTotal(), (int) page.getCurrent(), (int) page.getSize()));
    }

    @Operation(summary = "子评论列表", description = "获取指定父评论的子评论列表")
    @GetMapping("/parent/{parentId}")
    public Result<List<DesignComment>> getChildComments(@PathVariable Long parentId) {
        List<DesignComment> comments = designCommentService.getChildComments(parentId);
        return Result.success(comments);
    }

    @Operation(summary = "点赞评论", description = "切换评论点赞状态")
    @PostMapping("/{id}/like")
    public Result<Void> toggleCommentLike(@PathVariable Long id) {
        designCommentService.toggleCommentLike(id);
        return Result.success();
    }
}