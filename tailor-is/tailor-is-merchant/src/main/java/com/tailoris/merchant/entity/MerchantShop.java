package com.tailoris.merchant.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.tailoris.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("merchant_shop")
public class MerchantShop extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("merchant_id")
    private Long merchantId;

    @TableField("shop_name")
    private String shopName;

    @TableField("shop_logo")
    private String shopLogo;

    @TableField("shop_banner")
    private String shopBanner;

    @TableField("shop_desc")
    private String shopDesc;

    @TableField("shop_status")
    private Integer shopStatus;

    @TableField("decoration_config")
    private String decorationConfig;

    @TableField("shop_theme")
    private String shopTheme;

    @TableField("announcement")
    private String announcement;

    @TableField("contact_service")
    private String contactService;

    @TableField("province")
    private String province;

    @TableField("city")
    private String city;

    @TableField("district")
    private String district;

    @TableField("address")
    private String address;

    @TableField("longitude")
    private Double longitude;

    @TableField("latitude")
    private Double latitude;

    @TableField("shop_rating")
    private Double shopRating;

    @TableField("follower_count")
    private Integer followerCount;

    @TableField("product_count")
    private Integer productCount;

    @TableField("sales_count")
    private Integer salesCount;
}
