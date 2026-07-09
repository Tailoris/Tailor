package com.tailoris.admin.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CampaignStatusUpdateRequest {

    @NotNull(message = "状态不能为空")
    private Integer status;
}