package com.tailoris.community.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.tailoris.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("community_report_action")
public class CommunityReportAction extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("report_id")
    private Long reportId;

    @TableField("handler_id")
    private Long handlerId;

    @TableField("action_type")
    private Integer actionType;

    @TableField("action_reason")
    private String actionReason;

    @TableField("action_days")
    private Integer actionDays;
}
