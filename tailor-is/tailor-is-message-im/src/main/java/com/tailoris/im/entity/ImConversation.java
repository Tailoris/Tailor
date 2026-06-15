package com.tailoris.im.entity;

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
@TableName("im_conversation")
public class ImConversation extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 会话参与者1的用户ID */
    @TableField("user_id1")
    private Long userId1;

    /** 会话参与者2的用户ID */
    @TableField("user_id2")
    private Long userId2;

    /** 最后一条消息内容 */
    @TableField("last_message")
    private String lastMessage;

    /** 最后一条消息时间 */
    @TableField("last_message_at")
    private LocalDateTime lastMessageAt;

    /** 未读消息数 */
    @TableField("unread_count")
    private Integer unreadCount;
}