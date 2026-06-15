package com.tailoris.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "版型检查请求")
public class PatternCheckRequest {

    @NotNull(message = "版型ID不能为空")
    @Schema(description = "版型ID")
    private Long patternId;
}
