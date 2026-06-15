package com.tailoris.supply.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.tailoris.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("supply_match_record")
public class SupplyMatchRecord extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("demand_post_id")
    private Long demandPostId;

    @TableField("supply_post_id")
    private Long supplyPostId;

    @TableField("match_score")
    private Integer matchScore;

    @TableField("match_reason")
    private String matchReason;

    @TableField("status")
    private Integer status;

    @TableField("initiator_id")
    private Long initiatorId;

    @TableField("receiver_id")
    private Long receiverId;
}
