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
@TableName("shop_member")
public class ShopMember extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("shop_id")
    private Long shopId;

    @TableField("merchant_id")
    private Long merchantId;

    @TableField("level")
    private Integer level;

    @TableField("member_price_enabled")
    private Integer memberPriceEnabled;

    @TableField("total_consume")
    private BigDecimal totalConsume;

    @TableField("order_count")
    private Integer orderCount;

    @TableField("points")
    private Integer points;

    @TableField("join_time")
    private LocalDateTime joinTime;

    @TableField("expire_time")
    private LocalDateTime expireTime;

    @TableField("status")
    private Integer status;
}
