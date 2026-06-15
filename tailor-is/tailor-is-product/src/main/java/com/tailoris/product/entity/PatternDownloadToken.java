package com.tailoris.product.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 数字纸样下载记录 - PRD-008.
 *
 * <p>用户购买数字纸样后的下载凭证，含签名token、过期时间、下载次数限制。</p>
 *
 * @author Tailor IS Team
 * @since 1.0.0
 */
@Data
@TableName("pattern_download_token")
public class PatternDownloadToken implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("order_id")
    private Long orderId;

    @TableField("pattern_id")
    private Long patternId;

    @TableField("product_id")
    private Long productId;

    @TableField("token")
    private String token;

    @TableField("max_download_count")
    private Integer maxDownloadCount;

    @TableField("used_count")
    private Integer usedCount;

    @TableField("expire_time")
    private LocalDateTime expireTime;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableLogic
    @TableField("deleted")
    private Integer deleted;
}
