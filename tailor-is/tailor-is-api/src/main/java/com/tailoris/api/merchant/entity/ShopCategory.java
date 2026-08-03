package com.tailoris.api.merchant.entity;

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
@TableName("shop_category")
public class ShopCategory extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("parent_id")
    private Long parentId;

    @TableField("category_name")
    private String categoryName;

    @TableField("category_code")
    private String categoryCode;

    @TableField("sort_order")
    private Integer sortOrder;

    @TableField("commission_rate")
    private BigDecimal commissionRate;

    @TableField("deposit_min")
    private BigDecimal depositMin;

    @TableField("icon")
    private String icon;

    @TableField("description")
    private String description;

    @TableField("status")
    private Integer status;
}
