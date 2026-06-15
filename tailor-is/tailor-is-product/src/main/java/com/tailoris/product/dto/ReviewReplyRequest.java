package com.tailoris.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 商家回复评价请求 DTO - PRD-009.
 *
 * @author Tailor IS Team
 * @since 1.0.0
 */
@Data
public class ReviewReplyRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "回复内容不能为空")
    @Size(max = 500, message = "回复内容最多500字")
    private String content;
}
