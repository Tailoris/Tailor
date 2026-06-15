package com.tailoris.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
public class SkuRequest {

    private Long id;

    @NotNull(message = "商品ID不能为空")
    private Long productId;

    private String skuCode;

    private String barcode;

    private Map<String, String> attributes;

    private String attributeText;

    @NotNull(message = "价格不能为空")
    private BigDecimal price;

    private BigDecimal originalPrice;

    private BigDecimal costPrice;

    @NotNull(message = "库存不能为空")
    private Integer stock;

    private Integer warningStock;

    private BigDecimal weight;

    private String image;

    private Integer status;
}
