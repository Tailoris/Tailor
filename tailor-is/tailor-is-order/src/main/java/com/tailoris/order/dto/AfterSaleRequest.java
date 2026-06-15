package com.tailoris.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class AfterSaleRequest {

    @NotNull(message = "订单ID不能为空")
    private Long orderId;

    private Long orderItemId;

    @NotNull(message = "售后类型不能为空")
    private Integer ticketType;

    @NotBlank(message = "售后原因不能为空")
    private String reason;

    private String description;

    private List<String> images;

    private String videoUrl;

    @NotNull(message = "退款金额不能为空")
    private BigDecimal refundAmount;

    private Integer refundQuantity;
}
