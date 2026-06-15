package com.tailoris.product.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.tailoris.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "product_review", autoResultMap = true)
public class ProductReview extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("product_id")
    private Long productId;

    @TableField("sku_id")
    private Long skuId;

    @TableField("user_id")
    private Long userId;

    @TableField("order_id")
    private Long orderId;

    @TableField("order_item_id")
    private Long orderItemId;

    @TableField("rating")
    private Integer rating;

    @TableField("content")
    private String content;

    @TableField(value = "images", typeHandler = JacksonTypeHandler.class)
    private List<String> images;

    @TableField("video_url")
    private String videoUrl;

    @TableField("status")
    private Integer status;

    @TableField("is_anonymous")
    private Integer isAnonymous;

    @TableField("is_additional")
    private Integer isAdditional;

    @TableField("parent_id")
    private Long parentId;

    @TableField("merchant_reply")
    private String merchantReply;

    @TableField("merchant_reply_time")
    private java.time.LocalDateTime merchantReplyTime;

    @TableField("like_count")
    private Integer likeCount;

    /** 🔒 PRD-009: 评价图片URL列表 */
    @TableField(value = "image_urls", typeHandler = JacksonTypeHandler.class)
    private List<String> imageUrls;

    /** 🔒 PRD-009: 评价标签（如：款式好看、质量不错、发货快） */
    @TableField(value = "tags", typeHandler = JacksonTypeHandler.class)
    private List<String> tags;

    /** 🔒 PRD-009: 评价的SKU规格 */
    @TableField("sku_spec")
    private String skuSpec;

    /** 🔒 PRD-009: 有帮助数 */
    @TableField("helpful_count")
    private Integer helpfulCount;

    /** 🔒 PRD-009: 举报数 */
    @TableField("report_count")
    private Integer reportCount;

    /** 🔒 PRD-009: 是否精选 */
    @TableField("is_featured")
    private Integer isFeatured;

    /** 🔒 PRD-009: 敏感词触发次数 */
    @TableField("sensitive_word_hits")
    private Integer sensitiveWordHits;

    /** 🔒 PRD-007: 软删除标记 */
    @TableLogic
    @TableField("deleted")
    private Integer deleted;
}
