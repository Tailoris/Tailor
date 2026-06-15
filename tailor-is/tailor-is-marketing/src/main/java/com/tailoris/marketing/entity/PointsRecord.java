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
@TableName("points_record")
public class PointsRecord extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("points_change")
    private Integer pointsChange;

    @TableField("change_type")
    private Integer changeType;

    @TableField("related_type")
    private String relatedType;

    @TableField("related_id")
    private Long relatedId;

    @TableField("related_no")
    private String relatedNo;

    @TableField("description")
    private String description;

    @TableField("points_before")
    private Integer pointsBefore;

    @TableField("points_after")
    private Integer pointsAfter;

    @TableField("expire_time")
    private LocalDateTime expireTime;
}
