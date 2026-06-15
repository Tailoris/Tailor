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
@TableName("mkt_group_buy_member")
public class MktGroupBuyMember extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("instance_id")
    private Long instanceId;

    @TableField("user_id")
    private Long userId;

    @TableField("order_id")
    private Long orderId;

    @TableField("is_leader")
    private Integer isLeader;

    @TableField("join_time")
    private LocalDateTime joinTime;

    @TableField("status")
    private Integer status;

    @TableField("pay_time")
    private LocalDateTime payTime;
}
