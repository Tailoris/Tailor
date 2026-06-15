package com.tailoris.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "退款请求")
public class RefundRequest {

    @NotNull(message = "售后工单ID不能为空")
    @Schema(description = "售后工单ID")
    private Long ticketId;

    @NotNull(message = "退款金额不能为空")
    @DecimalMin(value = "0.01", message = "退款金额必须大于0")
    @Schema(description = "退款金额")
    private BigDecimal amount;

    @NotNull(message = "退款渠道不能为空")
    @Schema(description = "退款渠道：1-微信，2-支付宝，3-银行卡，4-余额")
    private Integer refundChannel;

    @Schema(description = "退款备注")
    private String remark;
}
