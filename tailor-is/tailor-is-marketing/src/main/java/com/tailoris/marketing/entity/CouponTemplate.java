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
@TableName("coupon_template")
public class CouponTemplate extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("name")
    private String name;

    @TableField("type")
    private Integer type;

    @TableField("discount_type")
    private Integer discountType;

    @TableField("discount_value")
    private BigDecimal discountValue;

    @TableField("min_amount")
    private BigDecimal minAmount;

    @TableField("max_discount")
    private BigDecimal maxDiscount;

    @TableField("total_count")
    private Integer totalCount;

    @TableField("issued_count")
    private Integer issuedCount;

    @TableField("received_count")
    private Integer receivedCount;

    @TableField("used_count")
    private Integer usedCount;

    @TableField("per_limit")
    private Integer perLimit;

    @TableField("scope_type")
    private Integer scopeType;

    @TableField("scope_value")
    private String scopeValue;

    @TableField("start_time")
    private LocalDateTime startTime;

    @TableField("end_time")
    private LocalDateTime endTime;

    @TableField("receive_start_time")
    private LocalDateTime receiveStartTime;

    @TableField("receive_end_time")
    private LocalDateTime receiveEndTime;

    @TableField("days_after_receive")
    private Integer daysAfterReceive;

    @TableField("status")
    private Integer status;

    @TableField("description")
    private String description;

    @TableField("remark")
    private String remark;
}
