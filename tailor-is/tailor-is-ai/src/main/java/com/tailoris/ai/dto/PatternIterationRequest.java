package com.tailoris.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "版型迭代请求")
public class PatternIterationRequest {

    @NotNull(message = "版型ID不能为空")
    @Schema(description = "版型ID")
    private Long patternId;

    @NotNull(message = "迭代类型不能为空")
    @Schema(description = "迭代类型：1-尺寸调整，2-样式修改，3-结构优化")
    private Integer iterationType;

    @Schema(description = "新参数(JSON)")
    private String newParameters;

    @Schema(description = "修改原因")
    private String changeReason;
}
