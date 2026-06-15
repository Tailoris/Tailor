package com.tailoris.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "仲裁请求")
public class ArbitrateRequest {

    @Schema(description = "售后工单ID")
    @NotNull(message = "工单ID不能为空")
    private Long ticketId;

    @Schema(description = "仲裁结果：1-支持买家，2-支持商家")
    @NotNull(message = "仲裁结果不能为空")
    private Integer result;

    @Schema(description = "仲裁备注")
    private String remark;
}
