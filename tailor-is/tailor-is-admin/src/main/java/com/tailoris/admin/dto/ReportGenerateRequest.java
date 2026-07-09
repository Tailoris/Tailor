package com.tailoris.admin.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ReportGenerateRequest {

    @NotBlank(message = "报告类型不能为空")
    private String reportType;

    private String startTime;

    private String endTime;

    private String reportName;

    private Long targetUserId;

    private String targetMerchantId;
}