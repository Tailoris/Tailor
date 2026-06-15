package com.tailoris.marketing.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.tailoris.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mkt_promotion_rule")
public class MktPromotionRule extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("promotion_name")
    private String promotionName;

    @TableField("promotion_type")
    private Integer promotionType;

    @TableField("shop_id")
    private Long shopId;

    @TableField("scope_type")
    private Integer scopeType;

    @TableField("scope_value")
    private String scopeValue;

    @TableField("threshold_type")
    private Integer thresholdType;

    @TableField("start_time")
    private LocalDateTime startTime;

    @TableField("end_time")
    private LocalDateTime endTime;

    @TableField("status")
    private Integer status;

    @TableField("priority")
    private Integer priority;

    @TableField("stackable")
    private Integer stackable;

    @TableField("description")
    private String description;
}
