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
 * 商家试运营考核记录 - MER-006.
 *
 * <p>商家入驻后30天试运营期内的关键指标考核记录。</p>
 *
 * <h3>考核维度</h3>
 * <ul>
 *   <li>订单量: 试运营期间有效订单数</li>
 *   <li>订单金额: 试运营期间总交易额</li>
 *   <li>商品丰富度: 上架商品数</li>
 *   <li>退款率: 退款单 / 总订单</li>
 *   <li>投诉数: 平台收到投诉</li>
 *   <li>违规次数: 平台判定违规</li>
 * </ul>
 *
 * <h3>考核结果</h3>
 * <ul>
 *   <li>PASS(1): 通过，转为正式商家</li>
 *   <li>FAIL(2): 未通过，关闭店铺</li>
 *   <li>EXTEND(3): 延期，再观察30天</li>
 * </ul>
 *
 * @author Tailor IS Team
 * @since 2026-06-03
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("merchant_trial_assessment")
public class MerchantTrialAssessment extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 商家ID */
    @TableField("merchant_id")
    private Long merchantId;

    /** 试运营开始日期 yyyy-MM-dd */
    @TableField("trial_start_date")
    private String trialStartDate;

    /** 试运营结束日期 yyyy-MM-dd */
    @TableField("trial_end_date")
    private String trialEndDate;

    /** 实际考核日期 yyyy-MM-dd */
    @TableField("assessment_date")
    private String assessmentDate;

    /** 试运营总天数（默认30） */
    @TableField("total_days")
    private Integer totalDays;

    /** 实际运营天数 */
    @TableField("actual_days")
    private Integer actualDays;

    /** 期间订单数 */
    @TableField("order_count")
    private Long orderCount;

    /** 期间订单金额 */
    @TableField("order_amount")
    private BigDecimal orderAmount;

    /** 上架商品数 */
    @TableField("product_count")
    private Long productCount;

    /** 退款率 0.0000-1.0000 */
    @TableField("refund_rate")
    private BigDecimal refundRate;

    /** 投诉数 */
    @TableField("complaint_count")
    private Long complaintCount;

    /** 违规次数 */
    @TableField("violation_count")
    private Long violationCount;

    /** 综合得分 0-100 */
    @TableField("score")
    private BigDecimal score;

    /** 考核结果 0=待考核 1=通过 2=未通过 3=延期 */
    @TableField("result")
    private Integer result;

    /** 是否已转正 0否 1是 */
    @TableField("is_promoted")
    private Integer isPromoted;

    /** 转正时间 */
    @TableField("promote_time")
    private String promoteTime;

    /** 考核备注 */
    @TableField("remark")
    private String remark;

    @TableLogic
    @TableField("deleted")
    private Integer deleted;
}
