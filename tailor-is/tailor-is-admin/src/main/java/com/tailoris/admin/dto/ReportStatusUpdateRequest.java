package com.tailoris.admin.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReportStatusUpdateRequest {

    @NotNull(message = "状态不能为空")
    private Integer status;

    private String remark;
}