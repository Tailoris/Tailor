package com.tailoris.product.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
public class ProductCreateRequest {

    @NotBlank(message = "商品名称不能为空")
    private String name;

    private String subTitle;

    @NotNull(message = "分类不能为空")
    private Long categoryId;

    @NotNull(message = "商品类型不能为空")
    private Integer productType;

    @NotNull(message = "商家ID不能为空")
    private Long merchantId;

    @NotNull(message = "店铺ID不能为空")
    private Long shopId;

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

    @Valid
    private List<SkuCreateRequest> skus;

    @Valid
    private List<AttributeCreateRequest> attributes;

    private List<Long> tagIds;

    @Data
    public static class SkuCreateRequest {
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
    public static class AttributeCreateRequest {
        private String attrName;
        private String attrValue;
        private Integer attrType;
        private Integer sort;
    }
}
