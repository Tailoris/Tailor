package com.tailoris.marketing.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.tailoris.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mkt_promotion_stats")
public class MktPromotionStats extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("promotion_type")
    private Integer promotionType;

    @TableField("promotion_id")
    private Long promotionId;

    @TableField("promotion_name")
    private String promotionName;

    @TableField("stat_date")
    private LocalDate statDate;

    @TableField("exposure_count")
    private Long exposureCount;

    @TableField("click_count")
    private Long clickCount;

    @TableField("participate_count")
    private Long participateCount;

    @TableField("order_count")
    private Long orderCount;

    @TableField("order_amount")
    private BigDecimal orderAmount;

    @TableField("discount_amount")
    private BigDecimal discountAmount;

    @TableField("roi")
    private BigDecimal roi;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;
}
