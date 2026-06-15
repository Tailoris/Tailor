package com.tailoris.marketing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
@Schema(description = "积分兑换请求")
public class PointsExchangeRequest {

    @NotNull(message = "积分商品ID不能为空")
    @Schema(description = "积分商城商品ID")
    private Long productId;

    @Positive(message = "兑换数量必须大于0")
    @Schema(description = "兑换数量")
    private Integer quantity;
}
