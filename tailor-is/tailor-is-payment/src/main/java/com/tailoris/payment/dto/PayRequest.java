package com.tailoris.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "创建支付请求")
public class PayRequest {

    @NotNull(message = "订单ID不能为空")
    @Schema(description = "订单ID")
    private Long orderId;

    @NotNull(message = "支付金额不能为空")
    @DecimalMin(value = "0.01", message = "支付金额必须大于0")
    @Schema(description = "支付金额")
    private BigDecimal amount;

    @NotNull(message = "支付渠道不能为空")
    @Schema(description = "支付渠道：1-微信，2-支付宝，3-银行卡，4-余额，5-Apple Pay，6-银联")
    private Integer payChannel;

    @Schema(description = "支付方式明细")
    private String payMethod;

    @Schema(description = "设备类型：1-PC，2-H5，3-小程序，4-APP")
    private Integer deviceType;

    @Schema(description = "异步回调地址")
    private String notifyUrl;

    @Schema(description = "支付备注")
    private String remark;

    @Schema(description = "微信OpenID")
    private String openId;

    @Schema(description = "商品描述(正文)")
    private String body;

    @Schema(description = "商品标题/摘要")
    private String subject;
}
