package com.tailoris.supply.entity;

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
@TableName("supply_supplier")
public class SupplySupplier extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("supplier_name")
    private String supplierName;

    @TableField("supplier_type")
    private Integer supplierType;

    @TableField("logo")
    private String logo;

    @TableField("description")
    private String description;

    @TableField("main_products")
    private String mainProducts;

    @TableField("main_categories")
    private String mainCategories;

    @TableField("location")
    private String location;

    @TableField("province")
    private String province;

    @TableField("city")
    private String city;

    @TableField("min_order_quantity")
    private Integer minOrderQuantity;

    @TableField("price_range")
    private String priceRange;

    @TableField("certifications")
    private String certifications;

    @TableField("rating")
    private BigDecimal rating;

    @TableField("order_count")
    private Integer orderCount;

    @TableField("response_rate")
    private BigDecimal responseRate;

    @TableField("contact_name")
    private String contactName;

    @TableField("contact_phone")
    private String contactPhone;

    @TableField("contact_wechat")
    private String contactWechat;

    @TableField("status")
    private Integer status;

    @TableField("is_verified")
    private Integer isVerified;

    @TableField("sort")
    private Integer sort;
}
