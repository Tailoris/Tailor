package com.tailoris.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "创建订单请求")
public class CreateOrderRequest {

    @NotEmpty(message = "购物车ID列表不能为空")
    @Schema(description = "购物车ID列表")
    private List<Long> cartIds;

    @Schema(description = "收货地址ID")
    private Long addressId;

    @Schema(description = "优惠券ID")
    private Long couponId;

    @Schema(description = "促销活动ID")
    private Long promotionId;

    @Schema(description = "订单备注")
    private String remark;

    /**
     * 客户端生成的幂等Key(UUID)，用于防止重复提交。
     * 同一requestId的请求30分钟内只会创建一个订单。
     */
    @NotBlank(message = "请求ID不能为空")
    @Schema(description = "幂等请求ID(客户端生成UUID)", example = "550e8400-e29b-41d4-a716-446655440000")
    private String requestId;
}
