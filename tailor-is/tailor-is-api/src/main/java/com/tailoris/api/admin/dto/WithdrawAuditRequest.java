package com.tailoris.api.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "提现审核请求")
public class WithdrawAuditRequest {

    @Schema(description = "提现申请ID")
    @NotNull(message = "提现申请ID不能为空")
    private Long withdrawId;

    @Schema(description = "是否通过")
    private Boolean approved;

    @Schema(description = "原因/备注")
    private String remark;
}
