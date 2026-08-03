package com.tailoris.api.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "保证金调整请求")
public class DepositAdjustRequest {

    @Schema(description = "商家ID")
    @NotNull(message = "商家ID不能为空")
    private Long merchantId;

    @Schema(description = "调整类型：1-补缴，2-调增，3-调减，4-退还")
    private Integer adjustType;

    @Schema(description = "调整金额")
    @NotNull(message = "调整金额不能为空")
    private BigDecimal amount;

    @Schema(description = "关联订单号")
    private String relatedOrderNo;

    @Schema(description = "备注")
    @NotBlank(message = "备注不能为空")
    private String remark;
}
