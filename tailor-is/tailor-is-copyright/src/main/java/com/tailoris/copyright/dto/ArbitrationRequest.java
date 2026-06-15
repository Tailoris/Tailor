package com.tailoris.copyright.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "仲裁请求")
public class ArbitrationRequest {

    @NotNull(message = "侵权举报ID不能为空")
    @Schema(description = "侵权举报ID")
    private Long infringementId;

    @Schema(description = "仲裁员ID")
    private Long arbitratorId;

    @Schema(description = "仲裁员姓名")
    private String arbitratorName;

    @Schema(description = "仲裁员类型：1-平台仲裁员，2-第三方，3-专家评审")
    private Integer arbitratorType;
}
