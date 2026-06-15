package com.tailoris.api.community.entity;

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
@TableName("community_post")
public class CommunityPost extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("title")
    private String title;

    @TableField("content")
    private String content;

    @TableField("images")
    private String images;

    @TableField("video_url")
    private String videoUrl;

    @TableField("type")
    private Integer type;

    @TableField("category_id")
    private Long categoryId;

    @TableField("tags")
    private String tags;

    @TableField("view_count")
    private Integer viewCount;

    @TableField("like_count")
    private Integer likeCount;

    @TableField("comment_count")
    private Integer commentCount;

    @TableField("share_count")
    private Integer shareCount;

    @TableField("collect_count")
    private Integer collectCount;

    @TableField("is_top")
    private Integer isTop;

    @TableField("is_essence")
    private Integer isEssence;

    @TableField("is_recommend")
    private Integer isRecommend;

    @TableField("status")
    private Integer status;

    @TableField("audit_status")
    private Integer auditStatus;

    @TableField("audit_remark")
    private String auditRemark;

    @TableField("audit_time")
    private LocalDateTime auditTime;

    @TableField("audit_by")
    private Long auditBy;

    @TableField("audit_user_id")
    private Long auditUserId;

    @TableField("related_product_id")
    private Long relatedProductId;

    @TableField("related_shop_id")
    private Long relatedShopId;

    @TableField("ip_address")
    private String ipAddress;

    @TableField("device_info")
    private String deviceInfo;

    @TableField("topic_ids")
    private String topicIds;

    @TableField("product_ids")
    private String productIds;

    @TableField("favorite_count")
    private Integer favoriteCount;

    @TableField("location")
    private String location;

    @TableField("longitude")
    private java.math.BigDecimal longitude;

    @TableField("latitude")
    private java.math.BigDecimal latitude;

    @TableField("summary")
    private String summary;
}
