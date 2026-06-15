package com.tailoris.product.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.tailoris.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "product_sku", autoResultMap = true)
public class ProductSku extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("product_id")
    private Long productId;

    @TableField("sku_code")
    private String skuCode;

    @TableField("barcode")
    private String barcode;

    @TableField(value = "attributes", typeHandler = JacksonTypeHandler.class)
    private Map<String, String> attributes;

    @TableField("attribute_text")
    private String attributeText;

    @TableField("price")
    private BigDecimal price;

    @TableField("original_price")
    private BigDecimal originalPrice;

    @TableField("cost_price")
    private BigDecimal costPrice;

    @TableField("stock")
    private Integer stock;

    @TableField("warning_stock")
    private Integer warningStock;

    @TableField("weight")
    private BigDecimal weight;

    @TableField("image")
    private String image;

    @TableField("status")
    private Integer status;

    @TableField("sales_count")
    private Integer salesCount;
}
