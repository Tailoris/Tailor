package com.tailoris.merchant.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.tailoris.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * Merchant notice/notification entity
 * <p>商家公告/通知实体，用于平台向商家推送系统公告、政策变更、活动通知等</p>
 *
 * @author Tailor IS Team
 * @since 2026-05-31
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("merchant_notice")
public class MerchantNotice extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    private String content;

    /**
     * Notice type: 1=system, 2=policy, 3=promo, 4=maintenance
     */
    private Integer noticeType;

    /**
     * Priority: 1=normal, 2=important, 3=urgent
     */
    private Integer priority;

    /**
     * Target: 0=all, merchant type filter
     */
    private Integer targetType;

    /**
     * Publisher ID
     */
    private Long publisherId;

    /**
     * Publisher name
     */
    private String publisherName;

    /**
     * Publish time
     */
    private LocalDateTime publishTime;

    /**
     * Read status (for current user)
     */
    private Boolean readStatus;

    /**
     * Read time (for current user)
     */
    private LocalDateTime readTime;

    /**
     * Status: 0=draft, 1=published, 2=withdrawn
     */
    private Integer status;

    @TableLogic
    private Integer deleted;
}
