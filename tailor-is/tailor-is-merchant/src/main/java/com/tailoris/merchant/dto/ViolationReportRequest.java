package com.tailoris.merchant.dto;

import com.tailoris.merchant.constant.ViolationConstants;
import lombok.Data;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.Serializable;

/**
 * 违规举报请求 - MER-007.
 *
 * @author Tailor IS Team
 * @since 2026-06-03
 */
@Data
public class ViolationReportRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "商家ID不能为空")
    private Long merchantId;

    private Long shopId;

    @NotNull(message = "违规类型不能为空")
    @Min(value = 1, message = "违规类型范围1-6")
    @Max(value = 6, message = "违规类型范围1-6")
    private Integer violationType;

    @NotBlank(message = "违规标题不能为空")
    @Size(max = 200, message = "标题长度不能超过200")
    private String title;

    @NotBlank(message = "违规描述不能为空")
    @Size(max = 2000, message = "描述长度不能超过2000")
    private String description;

    /** 违规证据（JSON字符串，图片URL列表） */
    private String evidence;

    /** 举报人ID（系统举报为空） */
    private Long reporterId;
}
