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
@TableName("im_message")
public class ImMessage extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 发送者用户ID */
    @TableField("from_user_id")
    private Long fromUserId;

    /** 接收者用户ID */
    @TableField("to_user_id")
    private Long toUserId;

    /** 消息内容 */
    @TableField("content")
    private String content;

    /** 消息类型：1文本 2图片 3语音 4文件 */
    @TableField("message_type")
    private Integer messageType;

    /** 会话ID */
    @TableField("conversation_id")
    private Long conversationId;

    /** 消息状态：0未读 1已读 */
    @TableField("status")
    private Integer status;

    /** 发送时间 */
    @TableField("sent_at")
    private LocalDateTime sentAt;
}