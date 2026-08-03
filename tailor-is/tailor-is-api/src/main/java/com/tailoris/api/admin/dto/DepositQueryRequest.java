package com.tailoris.api.admin.dto;

import com.tailoris.common.dto.PageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "保证金查询请求")
public class DepositQueryRequest extends PageRequest {

    @Schema(description = "商家ID")
    private Long merchantId;

    @Schema(description = "保证金状态（quality_deposit.status）")
    private Integer status;

    @Schema(description = "最低保证金额度")
    private BigDecimal minDeposit;

    @Schema(description = "最高保证金额度")
    private BigDecimal maxDeposit;
}
