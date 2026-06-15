package com.tailoris.marketing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "创建秒杀活动请求")
public class SeckillCreateRequest {

    @NotBlank(message = "活动名称不能为空")
    @Schema(description = "秒杀活动名称")
    private String name;

    @NotNull(message = "开始时间不能为空")
    @Schema(description = "活动开始时间")
    private LocalDateTime startTime;

    @NotNull(message = "结束时间不能为空")
    @Schema(description = "活动结束时间")
    private LocalDateTime endTime;

    @NotNull(message = "商品ID不能为空")
    @Schema(description = "商品ID")
    private Long productId;

    @NotNull(message = "SKU ID不能为空")
    @Schema(description = "SKU ID")
    private Long skuId;

    @NotNull(message = "秒杀价格不能为空")
    @DecimalMin(value = "0.01", message = "秒杀价格必须大于0")
    @Schema(description = "秒杀价格")
    private BigDecimal seckillPrice;

    @NotNull(message = "原价不能为空")
    @Schema(description = "原价")
    private BigDecimal originalPrice;

    @NotNull(message = "秒杀库存不能为空")
    @Schema(description = "秒杀库存")
    private Integer stock;

    @Schema(description = "每人限购数量")
    private Integer limitCount;

    @Schema(description = "排序权重")
    private Integer sort;

    @Schema(description = "活动描述")
    private String description;
}
