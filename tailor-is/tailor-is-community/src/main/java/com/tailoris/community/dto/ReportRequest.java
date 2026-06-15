package com.tailoris.community.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "举报请求")
public class ReportRequest {

    @Schema(description = "帖子ID")
    private Long postId;

    @Schema(description = "评论ID")
    private Long commentId;

    @NotNull(message = "被举报用户ID不能为空")
    @Schema(description = "被举报用户ID")
    private Long reportedUserId;

    @NotBlank(message = "举报原因不能为空")
    @Schema(description = "举报原因：广告、色情、暴力、政治敏感、侵权、其他")
    private String reason;

    @Schema(description = "举报详细描述")
    private String description;

    @Schema(description = "举证图片(JSON数组)")
    private String evidenceImages;
}
