package com.tailoris.payment.dto;

import com.tailoris.common.dto.PageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "结算查询请求")
public class SettlementQueryRequest extends PageRequest {

    @Schema(description = "商家ID")
    private Long merchantId;

    @Schema(description = "店铺ID")
    private Long shopId;

    @Schema(description = "结算状态：0-待结算，1-结算中，2-已结算，3-结算失败")
    private Integer status;

    @Schema(description = "结算类型：1-订单结算，2-提现结算，3-手动结算，4-批量结算")
    private Integer settlementType;

    @Schema(description = "开始时间")
    private String startTime;

    @Schema(description = "结束时间")
    private String endTime;
}
