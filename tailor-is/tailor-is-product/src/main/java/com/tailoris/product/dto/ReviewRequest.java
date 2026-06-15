package com.tailoris.product.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class ReviewRequest {

    @NotNull(message = "商品ID不能为空")
    private Long productId;

    private Long skuId;

    @NotNull(message = "订单ID不能为空")
    private Long orderId;

    private Long orderItemId;

    @NotNull(message = "评分不能为空")
    @Min(value = 1, message = "评分最小为1")
    @Max(value = 5, message = "评分最大为5")
    private Integer rating;

    @NotBlank(message = "评价内容不能为空")
    private String content;

    private List<String> images;

    private String videoUrl;

    private Integer isAnonymous;

    private Integer isAdditional;

    private Long parentId;
}
