package com.tailoris.order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CartAddRequest {

    @NotNull(message = "商品ID不能为空")
    private Long productId;

    @NotNull(message = "SKU ID不能为空")
    private Long skuId;

    @NotNull(message = "数量不能为空")
    @Min(value = 1, message = "数量最小为1")
    private Integer quantity;

    /** BE-M-33: 价格快照 - 加入购物车时的商品单价，下单时作为快照使用 */
    private BigDecimal priceSnapshot;
}
