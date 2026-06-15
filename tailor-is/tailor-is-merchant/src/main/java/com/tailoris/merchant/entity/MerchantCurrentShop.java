package com.tailoris.merchant.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 商家当前操作店铺 - MER-003.
 *
 * <p>记录某用户在某商家下的当前操作店铺，支持多店铺快速切换。</p>
 *
 * @author Tailor IS Team
 * @since 2026-06-03
 */
@Data
@TableName("merchant_current_shop")
public class MerchantCurrentShop implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    @TableField("user_id")
    private Long userId;

    /** 商家ID */
    @TableField("merchant_id")
    private Long merchantId;

    /** 当前操作的店铺ID */
    @TableField("current_shop_id")
    private Long currentShopId;

    /** 最近切换时间 */
    @TableField("last_switch_time")
    private LocalDateTime lastSwitchTime;

    @TableField("create_time")
    private LocalDateTime createTime;
}
