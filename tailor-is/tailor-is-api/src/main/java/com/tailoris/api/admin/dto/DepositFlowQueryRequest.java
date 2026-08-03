package com.tailoris.api.admin.dto;

import com.tailoris.common.dto.PageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "保证金流水查询请求")
public class DepositFlowQueryRequest extends PageRequest {

    @Schema(description = "商家ID")
    private Long merchantId;

    @Schema(description = "流水类型")
    private Integer flowType;

    @Schema(description = "开始日期")
    private LocalDateTime startDate;

    @Schema(description = "结束日期")
    private LocalDateTime endDate;
}
