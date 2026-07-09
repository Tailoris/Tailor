package com.tailoris.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CouponCreateRequest {

    @NotBlank(message = "优惠券名称不能为空")
    private String couponName;

    private String description;

    @NotNull(message = "类型不能为空")
    private Integer type;

    @NotNull(message = "金额不能为空")
    private BigDecimal amount;

    private BigDecimal minAmount;

    @NotNull(message = "数量不能为空")
    private Integer quantity;

    private String startTime;

    private String endTime;

    private Integer status;
}