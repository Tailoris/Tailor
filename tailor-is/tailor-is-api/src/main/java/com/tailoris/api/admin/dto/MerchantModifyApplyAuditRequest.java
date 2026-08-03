package com.tailoris.api.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "商家修改申请审核请求")
public class MerchantModifyApplyAuditRequest {

    @Schema(description = "申请ID")
    @NotNull(message = "申请ID不能为空")
    private Long applyId;

    @Schema(description = "是否通过")
    @NotNull(message = "审核结果不能为空")
    private Boolean approved;

    @Schema(description = "审核备注")
    private String remark;
}
