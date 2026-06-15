package com.tailoris.marketing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "领取优惠券请求")
public class CouponReceiveRequest {

    @NotNull(message = "优惠券模板ID不能为空")
    @Schema(description = "优惠券模板ID")
    private Long couponId;
}
