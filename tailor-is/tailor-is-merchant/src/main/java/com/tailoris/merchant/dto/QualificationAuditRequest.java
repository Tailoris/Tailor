package com.tailoris.merchant.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "资质审核请求")
public class QualificationAuditRequest {

    @Schema(description = "资质ID")
    @NotNull(message = "资质ID不能为空")
    private Long qualificationId;

    @Schema(description = "审核状态：0-待审核，1-审核中，2-已通过，3-已驳回")
    @NotNull(message = "审核状态不能为空")
    private Integer auditStatus;

    @Schema(description = "审核备注")
    @Size(max = 512, message = "审核备注不能超过512个字符")
    private String auditRemark;
}
