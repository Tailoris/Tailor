package com.tailoris.product.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 创建商品评价请求 DTO - PRD-009.
 *
 * @author Tailor IS Team
 * @since 1.0.0
 */
@Data
public class CreateReviewRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "商品ID不能为空")
    private Long productId;

    @NotNull(message = "订单ID不能为空")
    private Long orderId;

    private Long skuId;

    private String skuSpec;

    @NotNull(message = "评分不能为空")
    @Min(value = 1, message = "评分应在1-5之间")
    @Max(value = 5, message = "评分应在1-5之间")
    private Integer rating;

    @NotBlank(message = "评价内容不能为空")
    @Size(min = 5, max = 1000, message = "评价内容应在5-1000字之间")
    private String content;

    /** 评价图片URL列表（最多9张） */
    @Size(max = 9, message = "最多上传9张图片")
    private List<String> imageUrls;

    /** 评价标签 */
    private List<String> tags;

    /** 是否匿名 */
    private Boolean isAnonymous;
}
