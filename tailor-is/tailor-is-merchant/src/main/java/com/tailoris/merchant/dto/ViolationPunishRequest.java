package com.tailoris.merchant.dto;

import lombok.Data;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.Serializable;

/**
 * 违规处罚请求（管理员） - MER-007.
 *
 * @author Tailor IS Team
 * @since 2026-06-03
 */
@Data
public class ViolationPunishRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "违规记录ID不能为空")
    private Long violationId;

    @NotNull(message = "违规级别不能为空")
    @Min(value = 1, message = "违规级别1-4")
    @Max(value = 4, message = "违规级别1-4")
    private Integer violationLevel;

    @NotNull(message = "处罚类型不能为空")
    @Min(value = 1, message = "处罚类型1-5")
    @Max(value = 5, message = "处罚类型1-5")
    private Integer punishmentType;

    @Min(value = 0, message = "处罚天数不能为负数（0表示永久）")
    private Integer punishmentDays;

    @Size(max = 500, message = "处罚说明不能超过500字")
    private String handleRemark;

    private Long handlerId;
}
