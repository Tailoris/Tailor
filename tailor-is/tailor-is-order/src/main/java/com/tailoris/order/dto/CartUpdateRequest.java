package com.tailoris.order.dto;

import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class CartUpdateRequest {

    @Min(value = 1, message = "数量最小为1")
    private Integer quantity;

    private Integer checked;
}
