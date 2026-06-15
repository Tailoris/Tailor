package com.tailoris.api.community.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.tailoris.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("community_report")
public class CommunityReport extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("post_id")
    private Long postId;

    @TableField("comment_id")
    private Long commentId;

    @TableField("reported_user_id")
    private Long reportedUserId;

    @TableField("reporter_id")
    private Long reporterId;

    @TableField("reason")
    private String reason;

    @TableField("description")
    private String description;

    @TableField("evidence_images")
    private String evidenceImages;

    @TableField("status")
    private Integer status;

    @TableField("handler_id")
    private Long handlerId;

    @TableField("handler_remark")
    private String handlerRemark;

    @TableField("handled_at")
    private java.time.LocalDateTime handledAt;

    @TableField("punishment_type")
    private Integer punishmentType;

    @TableField("target_id")
    private Long targetId;

    @TableField("target_type")
    private Integer targetType;

    @TableField("reason_type")
    private Integer reasonType;

    @TableField("evidence")
    private String evidence;

    @TableField("handle_result")
    private String handleResult;

    @TableField("handle_time")
    private java.time.LocalDateTime handleTime;
}
