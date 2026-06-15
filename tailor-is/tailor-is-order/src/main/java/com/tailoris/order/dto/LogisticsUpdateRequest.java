package com.tailoris.order.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LogisticsUpdateRequest {

    @NotBlank(message = "物流公司编码不能为空")
    private String logisticsCompany;

    private String logisticsCompanyName;

    @NotBlank(message = "物流单号不能为空")
    private String logisticsNo;
}
