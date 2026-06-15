package com.tailoris.marketing.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.tailoris.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("points_mall_product")
public class PointsMallProduct extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("name")
    private String name;

    @TableField("image")
    private String image;

    @TableField("images")
    private String images;

    @TableField("description")
    private String description;

    @TableField("points_required")
    private Integer pointsRequired;

    @TableField("cash_price")
    private java.math.BigDecimal cashPrice;

    @TableField("stock")
    private Integer stock;

    @TableField("limit_count")
    private Integer limitCount;

    @TableField("status")
    private Integer status;

    @TableField("sort")
    private Integer sort;

    @TableField("exchange_count")
    private Integer exchangeCount;
}
