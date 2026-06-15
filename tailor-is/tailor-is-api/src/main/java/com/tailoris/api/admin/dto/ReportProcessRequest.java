package com.tailoris.api.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "举报处理请求")
public class ReportProcessRequest {

    @Schema(description = "举报ID")
    @NotNull(message = "举报ID不能为空")
    private Long reportId;

    @Schema(description = "处理结果类型：1-已处理，2-忽略")
    @NotNull(message = "处理结果不能为空")
    private Integer processResult;

    @Schema(description = "处理备注")
    @Size(max = 512, message = "处理备注不能超过512个字符")
    private String remark;
}
