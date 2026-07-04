package com.tailoris.order.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class CartCheckoutRequest {

    @NotEmpty(message = "购物车ID列表不能为空")
    private List<Long> ids;
}