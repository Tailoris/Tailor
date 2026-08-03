package com.tailoris.api.admin.dto;

import com.tailoris.common.dto.PageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "商家修改申请查询请求")
public class MerchantModifyApplyQueryRequest extends PageRequest {

    @Schema(description = "商家ID")
    private Long merchantId;

    @Schema(description = "申请类型")
    private Integer applyType;

    @Schema(description = "状态：0-待审核，1-审核中，2-已通过，3-已驳回")
    private Integer status;

    @Schema(description = "开始日期")
    private LocalDateTime startDate;

    @Schema(description = "结束日期")
    private LocalDateTime endDate;

    @Schema(description = "关键词（申请单号模糊查询）")
    private String keyword;
}
