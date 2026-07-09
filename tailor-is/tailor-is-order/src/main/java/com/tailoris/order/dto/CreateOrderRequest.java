package com.tailoris.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Schema(description = "创建订单请求")
public class CreateOrderRequest {

    @Schema(description = "购物车ID列表（与items二选一）")
    private List<Long> cartIds;

    @Schema(description = "商品项列表（与cartIds二选一，直接购买模式）")
    private List<OrderItemRequest> items;

    @NotNull(message = "收货地址ID不能为空")
    @Positive(message = "收货地址ID必须为正数")
    @Schema(description = "收货地址ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long addressId;

    @Schema(description = "优惠券ID")
    private Long couponId;

    @Schema(description = "促销活动ID")
    private Long promotionId;

    @Schema(description = "订单备注")
    private String remark;

    @NotBlank(message = "请求ID不能为空")
    @Schema(description = "幂等请求ID(客户端生成UUID)", example = "550e8400-e29b-41d4-a716-446655440000", requiredMode = Schema.RequiredMode.REQUIRED)
    private String requestId;

    @Data
    @Schema(description = "订单商品项请求")
    public static class OrderItemRequest {

        @NotNull(message = "商品ID不能为空")
        @Positive(message = "商品ID必须为正数")
        @Schema(description = "商品ID", requiredMode = Schema.RequiredMode.REQUIRED)
        private Long productId;

        @NotNull(message = "SKU ID不能为空")
        @Positive(message = "SKU ID必须为正数")
        @Schema(description = "SKU ID", requiredMode = Schema.RequiredMode.REQUIRED)
        private Long skuId;

        @NotNull(message = "数量不能为空")
        @Positive(message = "数量必须为正数")
        @Schema(description = "购买数量", requiredMode = Schema.RequiredMode.REQUIRED)
        private Integer quantity;

        @Schema(description = "价格快照")
        private BigDecimal price;

        @Schema(description = "店铺ID")
        private Long shopId;

        @Schema(description = "商户ID")
        private Long merchantId;
    }
}
