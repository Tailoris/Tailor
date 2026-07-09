package com.tailoris.admin.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SupplierAuditRequest {

    @NotNull(message = "审核状态不能为空")
    private Integer status;

    private String auditRemark;
}