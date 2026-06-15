package com.tailoris.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "商家审核请求")
public class MerchantAuditRequest {

    @Schema(description = "商家ID")
    @NotNull(message = "商家ID不能为空")
    private Long merchantId;

    @Schema(description = "审核状态：2-通过，3-驳回")
    @NotNull(message = "审核状态不能为空")
    private Integer auditStatus;

    @Schema(description = "审核备注")
    @Size(max = 512, message = "审核备注不能超过512个字符")
    private String auditRemark;
}
