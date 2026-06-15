package com.tailoris.community.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "点赞/取消点赞请求")
public class LikeRequest {

    @NotNull(message = "目标类型不能为空")
    @Schema(description = "目标类型：1-帖子，2-评论")
    private Integer targetType;

    @NotNull(message = "目标ID不能为空")
    @Schema(description = "目标ID")
    private Long targetId;
}
