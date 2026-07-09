package com.tailoris.design.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.tailoris.common.dto.PageResponse;
import com.tailoris.common.result.Result;
import com.tailoris.design.dto.CommentRequest;
import com.tailoris.design.entity.DesignComment;
import com.tailoris.design.service.DesignCommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/design/comments")
@RequiredArgsConstructor
public class DesignCommentController {

    private final DesignCommentService designCommentService;

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

    @DeleteMapping("/{id}")
    public Result<Void> deleteComment(@PathVariable Long id) {
        designCommentService.deleteComment(id);
        return Result.success();
    }

    @GetMapping("/design/{designId}")
    public Result<PageResponse<DesignComment>> getCommentsByDesignId(
            @PathVariable Long designId,
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size) {
        IPage<DesignComment> page = designCommentService.getCommentsByDesignId(designId, current, size);
        return Result.success(PageResponse.of(page));
    }

    @GetMapping("/parent/{parentId}")
    public Result<List<DesignComment>> getChildComments(@PathVariable Long parentId) {
        List<DesignComment> comments = designCommentService.getChildComments(parentId);
        return Result.success(comments);
    }

    @PostMapping("/{id}/like")
    public Result<Void> toggleCommentLike(@PathVariable Long id) {
        designCommentService.toggleCommentLike(id);
        return Result.success();
    }
}