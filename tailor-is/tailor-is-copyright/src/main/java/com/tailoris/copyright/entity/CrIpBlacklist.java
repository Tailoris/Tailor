package com.tailoris.copyright.entity;

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
@TableName("cr_ip_blacklist")
public class CrIpBlacklist extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("list_type")
    private Integer listType;

    @TableField("target_type")
    private Integer targetType;

    @TableField("target_value")
    private String targetValue;

    @TableField("reason")
    private String reason;

    @TableField("evidence")
    private String evidence;

    @TableField("expire_time")
    private LocalDateTime expireTime;

    @TableField("operator_id")
    private Long operatorId;
}
