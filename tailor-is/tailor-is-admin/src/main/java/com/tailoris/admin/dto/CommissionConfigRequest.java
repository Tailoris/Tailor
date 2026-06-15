package com.tailoris.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "佣金配置请求")
public class CommissionConfigRequest {

    @Schema(description = "商家类型：1-个人，2-企业，3-个体工商户")
    @NotNull(message = "商家类型不能为空")
    private Integer merchantType;

    @Schema(description = "佣金比例（0-1之间的小数）")
    @NotNull(message = "佣金比例不能为空")
    @DecimalMin(value = "0.00", message = "佣金比例最小为0")
    @DecimalMax(value = "1.00", message = "佣金比例最大为1")
    private BigDecimal commissionRate;
}
