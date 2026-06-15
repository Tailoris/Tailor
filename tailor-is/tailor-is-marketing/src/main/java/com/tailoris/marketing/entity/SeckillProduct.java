package com.tailoris.marketing.entity;

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
@TableName("seckill_product")
public class SeckillProduct extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("activity_id")
    private Long activityId;

    @TableField("product_id")
    private Long productId;

    @TableField("sku_id")
    private Long skuId;

    @TableField("seckill_price")
    private BigDecimal seckillPrice;

    @TableField("original_price")
    private BigDecimal originalPrice;

    @TableField("stock")
    private Integer stock;

    @TableField("available_stock")
    private Integer availableStock;

    @TableField("limit_count")
    private Integer limitCount;

    @TableField("sort")
    private Integer sort;

    @TableField("status")
    private Integer status;

    @TableField("order_count")
    private Integer orderCount;
}
