package com.tailoris.merchant.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.Serializable;

/**
 * 违规申诉请求 - MER-007.
 *
 * @author Tailor IS Team
 * @since 2026-06-03
 */
@Data
public class ViolationAppealRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "违规记录ID不能为空")
    private Long violationId;

    @NotNull(message = "申诉商家ID不能为空")
    private Long merchantId;

    @NotBlank(message = "申诉内容不能为空")
    @Size(min = 10, max = 2000, message = "申诉内容长度10-2000字")
    private String appealContent;

    /** 申诉证据 */
    private String evidence;
}
