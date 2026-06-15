package com.tailoris.marketing.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.tailoris.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mkt_group_buy")
public class MktGroupBuy extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("activity_name")
    private String activityName;

    @TableField("product_id")
    private Long productId;

    @TableField("sku_id")
    private Long skuId;

    @TableField("shop_id")
    private Long shopId;

    @TableField("group_size")
    private Integer groupSize;

    @TableField("group_price")
    private BigDecimal groupPrice;

    @TableField("original_price")
    private BigDecimal originalPrice;

    @TableField("total_stock")
    private Integer totalStock;

    @TableField("sold_count")
    private Integer soldCount;

    @TableField("group_count")
    private Integer groupCount;

    @TableField("success_count")
    private Integer successCount;

    @TableField("limit_per_user")
    private Integer limitPerUser;

    @TableField("valid_hours")
    private Integer validHours;

    @TableField("start_time")
    private LocalDateTime startTime;

    @TableField("end_time")
    private LocalDateTime endTime;

    @TableField("status")
    private Integer status;

    @TableField("description")
    private String description;
}
