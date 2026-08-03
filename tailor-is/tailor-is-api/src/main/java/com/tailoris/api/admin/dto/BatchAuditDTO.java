package com.tailoris.api.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "批量审核DTO")
public class BatchAuditDTO {

    @Schema(description = "商家ID列表")
    @NotEmpty(message = "商家ID列表不能为空")
    private List<Long> ids;

    @Schema(description = "是否通过")
    @NotNull(message = "审核结果不能为空")
    private Boolean approved;

    @Schema(description = "审核备注")
    private String remark;
}
