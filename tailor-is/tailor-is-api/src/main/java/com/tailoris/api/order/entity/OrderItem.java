package com.tailoris.api.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.tailoris.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("order_item")
public class OrderItem extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("order_id")
    private Long orderId;

    @TableField("order_no")
    private String orderNo;

    @TableField("product_id")
    private Long productId;

    @TableField("sku_id")
    private Long skuId;

    @TableField("product_name")
    private String productName;

    @TableField("product_image")
    private String productImage;

    @TableField("sku_attributes")
    private String skuAttributes;

    @TableField("sku_attribute_text")
    private String skuAttributeText;

    @TableField("quantity")
    private Integer quantity;

    @TableField("price")
    private BigDecimal price;

    @TableField("subtotal")
    private BigDecimal subtotal;

    @TableField("discount_amount")
    private BigDecimal discountAmount;

    @TableField("pay_amount")
    private BigDecimal payAmount;

    @TableField("is_commented")
    private Integer isCommented;

    @TableField("after_sale_status")
    private Integer afterSaleStatus;
}
