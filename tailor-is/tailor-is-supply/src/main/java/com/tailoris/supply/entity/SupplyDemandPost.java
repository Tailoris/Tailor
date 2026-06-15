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
@TableName("supply_demand_post")
public class SupplyDemandPost extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("post_type")
    private Integer postType;

    @TableField("title")
    private String title;

    @TableField("content")
    private String content;

    @TableField("images")
    private String images;

    @TableField("category_id")
    private Long categoryId;

    @TableField("material_type")
    private String materialType;

    @TableField("quantity")
    private Integer quantity;

    @TableField("unit_price")
    private BigDecimal unitPrice;

    @TableField("location")
    private String location;

    @TableField("province")
    private String province;

    @TableField("city")
    private String city;

    @TableField("contact_name")
    private String contactName;

    @TableField("contact_phone")
    private String contactPhone;

    @TableField("contact_wechat")
    private String contactWechat;

    @TableField("expire_date")
    private java.time.LocalDate expireDate;

    @TableField("view_count")
    private Integer viewCount;

    @TableField("contact_count")
    private Integer contactCount;

    @TableField("status")
    private Integer status;

    @TableField("is_top")
    private Integer isTop;

    @TableField("is_urgent")
    private Integer isUrgent;
}
