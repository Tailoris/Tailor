package com.tailoris.supply.entity;

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
@TableName("supply_contact_record")
public class SupplyContactRecord extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("post_id")
    private Long postId;

    @TableField("from_user_id")
    private Long fromUserId;

    @TableField("to_user_id")
    private Long toUserId;

    @TableField("message")
    private String message;

    @TableField("contact_method")
    private String contactMethod;

    @TableField("status")
    private Integer status;

    @TableField("reply_message")
    private String replyMessage;

    @TableField("reply_time")
    private LocalDateTime replyTime;

    @TableField("is_read")
    private Integer isRead;
}
