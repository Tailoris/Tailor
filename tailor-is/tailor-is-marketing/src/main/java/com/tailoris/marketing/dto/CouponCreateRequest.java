package com.tailoris.marketing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "创建优惠券请求")
public class CouponCreateRequest {

    @NotBlank(message = "优惠券名称不能为空")
    @Schema(description = "优惠券名称")
    private String name;

    @NotNull(message = "优惠券类型不能为空")
    @Schema(description = "优惠券类型：1-满减券，2-折扣券，3-立减券，4-运费券")
    private Integer type;

    @NotNull(message = "优惠类型不能为空")
    @Schema(description = "优惠类型：1-固定金额，2-百分比折扣")
    private Integer discountType;

    @NotNull(message = "优惠值不能为空")
    @DecimalMin(value = "0.01", message = "优惠值必须大于0")
    @Schema(description = "优惠面额/折扣值")
    private BigDecimal discountValue;

    @Schema(description = "最低消费金额")
    private BigDecimal minAmount;

    @Schema(description = "最大抵扣金额")
    private BigDecimal maxDiscount;

    @NotNull(message = "发放总量不能为空")
    @Schema(description = "发放总量（-1表示不限量）")
    private Integer totalCount;

    @Schema(description = "每人限领数量")
    private Integer perLimit;

    @Schema(description = "适用范围：1-全场通用，2-指定分类，3-指定商品，4-指定店铺")
    private Integer scopeType;

    @Schema(description = "适用范围值（JSON数组）")
    private String scopeValue;

    @NotNull(message = "开始时间不能为空")
    @Schema(description = "生效开始时间")
    private LocalDateTime startTime;

    @NotNull(message = "结束时间不能为空")
    @Schema(description = "生效结束时间")
    private LocalDateTime endTime;

    @Schema(description = "优惠券说明")
    private String description;

    @Schema(description = "领取开始时间")
    private LocalDateTime receiveStartTime;

    @Schema(description = "领取结束时间")
    private LocalDateTime receiveEndTime;
}
