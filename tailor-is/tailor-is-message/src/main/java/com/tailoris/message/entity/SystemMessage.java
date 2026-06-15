package com.tailoris.message.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.tailoris.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("system_message")
public class SystemMessage extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("title")
    private String title;

    @TableField("content")
    private String content;

    @TableField("type")
    private Integer type;

    @TableField("priority")
    private Integer priority;

    @TableField("sender_id")
    private Long senderId;

    @TableField("sender_type")
    private Integer senderType;

    @TableField("target_type")
    private Integer targetType;

    @TableField("target_user_id")
    private Long targetUserId;

    @TableField("target_merchant_id")
    private Long targetMerchantId;

    @TableField("related_type")
    private String relatedType;

    @TableField("related_id")
    private Long relatedId;

    @TableField("business_no")
    private String businessNo;

    @TableField("is_push")
    private Integer isPush;

    @TableField("push_status")
    private Integer pushStatus;

    @TableField("status")
    private Integer status;
}
