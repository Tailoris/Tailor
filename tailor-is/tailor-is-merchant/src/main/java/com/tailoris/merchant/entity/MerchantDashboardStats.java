package com.tailoris.merchant.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.tailoris.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 商家数据工作台统计实体 - MER-005.
 *
 * <p>按日/周/月维度统计商家核心经营指标。</p>
 *
 * <h3>核心指标</h3>
 * <ul>
 *   <li>流量类: PV/UV/商品浏览/店铺关注</li>
 *   <li>转化类: 加购/订单/转化率</li>
 *   <li>交易类: 订单数/订单金额/退款</li>
 *   <li>收益类: 已支付订单金额</li>
 * </ul>
 *
 * @author Tailor IS Team
 * @since 2026-06-03
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("merchant_dashboard_stats")
public class MerchantDashboardStats extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 商家ID */
    @TableField("merchant_id")
    private Long merchantId;

    /** 店铺ID（为空表示全店汇总） */
    @TableField("shop_id")
    private Long shopId;

    /** 统计日期 yyyy-MM-dd */
    @TableField("stat_date")
    private String statDate;

    /** 统计类型 1=日 2=周 3=月 */
    @TableField("stat_type")
    private Integer statType;

    /** 浏览量PV */
    @TableField("pv_count")
    private Long pvCount;

    /** 访客数UV */
    @TableField("uv_count")
    private Long uvCount;

    /** 商品详情浏览数 */
    @TableField("product_view_count")
    private Long productViewCount;

    /** 店铺关注数 */
    @TableField("shop_follow_count")
    private Long shopFollowCount;

    /** 加购数 */
    @TableField("cart_add_count")
    private Long cartAddCount;

    /** 订单数 */
    @TableField("order_count")
    private Long orderCount;

    /** 订单金额 */
    @TableField("order_amount")
    private BigDecimal orderAmount;

    /** 已支付订单数 */
    @TableField("paid_order_count")
    private Long paidOrderCount;

    /** 已支付订单金额 */
    @TableField("paid_order_amount")
    private BigDecimal paidOrderAmount;

    /** 退款单数 */
    @TableField("refund_count")
    private Long refundCount;

    /** 退款金额 */
    @TableField("refund_amount")
    private BigDecimal refundAmount;

    /** 转化率（0.0000-1.0000） */
    @TableField("conversion_rate")
    private BigDecimal conversionRate;

    @TableLogic
    @TableField("deleted")
    private Integer deleted;
}
