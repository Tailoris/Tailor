package com.tailoris.community.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "评论请求")
public class CommentRequest {

    @NotNull(message = "帖子ID不能为空")
    @Schema(description = "帖子ID")
    private Long postId;

    @NotBlank(message = "评论内容不能为空")
    @Schema(description = "评论内容")
    private String content;

    @Schema(description = "图片URL列表(JSON数组)")
    private String images;

    @Schema(description = "父评论ID(回复评论时使用)")
    private Long parentId;

    @Schema(description = "被回复的用户ID")
    private Long replyToUserId;
}
