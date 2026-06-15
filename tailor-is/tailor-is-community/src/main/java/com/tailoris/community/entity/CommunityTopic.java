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
@TableName("community_topic")
public class CommunityTopic extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("topic_name")
    private String topicName;

    @TableField("topic_desc")
    private String topicDesc;

    @TableField("cover_image")
    private String coverImage;

    @TableField("creator_id")
    private Long creatorId;

    @TableField("post_count")
    private Integer postCount;

    @TableField("follow_count")
    private Integer followCount;

    @TableField("view_count")
    private Long viewCount;

    @TableField("is_hot")
    private Integer isHot;

    @TableField("is_official")
    private Integer isOfficial;

    @TableField("sort_order")
    private Integer sortOrder;

    @TableField("status")
    private Integer status;
}
