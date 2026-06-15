package com.tailoris.api.admin.dto;

import com.tailoris.common.dto.PageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "订单查询请求")
public class OrderQueryRequest extends PageRequest {

    @Schema(description = "关键词（订单号）")
    private String keyword;

    @Schema(description = "订单状态")
    private Integer status;

    @Schema(description = "商品类型")
    private Integer productType;
}
