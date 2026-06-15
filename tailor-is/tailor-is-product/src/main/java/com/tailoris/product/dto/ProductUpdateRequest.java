package com.tailoris.product.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
public class ProductUpdateRequest {

    @NotBlank(message = "商品名称不能为空")
    private String name;

    private String subTitle;

    private Long categoryId;

    private Integer productType;

    @NotBlank(message = "商品主图不能为空")
    private String mainImage;

    private List<String> images;

    private String videoUrl;

    private String description;

    private Object specifications;

    private Integer copyrightFlag;

    private Long copyrightId;

    private String brandName;

    private BigDecimal weight;

    private BigDecimal length;

    private BigDecimal width;

    private BigDecimal height;

    private Long freightTemplateId;

    private String lowerShelfReason;

    @Valid
    private List<SkuUpdateRequest> skus;

    @Valid
    private List<AttributeUpdateRequest> attributes;

    private List<Long> tagIds;

    @Data
    public static class SkuUpdateRequest {
        private Long id;
        private String skuCode;
        private String barcode;
        private Map<String, String> attributes;
        private String attributeText;
        private BigDecimal price;
        private BigDecimal originalPrice;
        private BigDecimal costPrice;
        private Integer stock;
        private Integer warningStock;
        private BigDecimal weight;
        private String image;
        private Integer status;
    }

    @Data
    public static class AttributeUpdateRequest {
        private Long id;
        private String attrName;
        private String attrValue;
        private Integer attrType;
        private Integer sort;
    }
}
