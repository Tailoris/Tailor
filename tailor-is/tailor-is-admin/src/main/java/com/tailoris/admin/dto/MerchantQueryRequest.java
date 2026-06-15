package com.tailoris.admin.dto;

import com.tailoris.common.dto.PageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "商家查询请求")
public class MerchantQueryRequest extends PageRequest {

    @Schema(description = "商家类型：1-个人，2-企业，3-个体工商户")
    private Integer merchantType;

    @Schema(description = "审核状态：0-待审核，1-审核中，2-已通过，3-已驳回")
    private Integer auditStatus;

    @Schema(description = "状态：0-待审核，1-正常，2-冻结，3-注销")
    private Integer status;

    @Schema(description = "关键词（公司名称/联系人姓名）")
    private String keyword;
}
