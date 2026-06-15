package com.tailoris.supply.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "联系请求")
public class ContactRequest {

    @NotNull(message = "帖子ID不能为空")
    @Schema(description = "帖子ID")
    private Long postId;

    @NotBlank(message = "联系内容不能为空")
    @Schema(description = "联系消息内容")
    private String message;

    @Schema(description = "联系方式")
    private String contactMethod;
}
