package com.tailoris.api.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Schema(description = "入驻类型DTO")
public class ApplyTypeDTO {

    @Schema(description = "类型编码")
    private String typeCode;

    @Schema(description = "类型名称")
    private String typeName;

    @Schema(description = "所需资质ID列表")
    private List<Integer> requiredQualifications;

    @Schema(description = "保证金额度")
    private BigDecimal depositAmount;

    @Schema(description = "额外佣金比例")
    private BigDecimal commissionExtraRate;

    @Schema(description = "是否需要审核：0-否，1-是")
    private Integer auditRequired;

    @Schema(description = "试运营天数")
    private Integer trialDays;

    @Schema(description = "排序")
    private Integer sortOrder;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "状态：0-禁用，1-启用")
    private Integer status;
}
