package com.tailoris.merchant.constant;

import java.math.BigDecimal;

/**
 * 商家试运营考核相关常量 - MER-006.
 *
 * @author Tailor IS Team
 * @since 2026-06-03
 */
public final class TrialAssessmentConstants {

    private TrialAssessmentConstants() {
    }

    /** 试运营总天数 */
    public static final int TRIAL_DAYS = 30;

    /** 考核结果：待考核 */
    public static final int RESULT_PENDING = 0;

    /** 考核结果：通过 */
    public static final int RESULT_PASS = 1;

    /** 考核结果：未通过 */
    public static final int RESULT_FAIL = 2;

    /** 考核结果：延期 */
    public static final int RESULT_EXTEND = 3;

    /** 评分阈值：通过（>=80） */
    public static final BigDecimal PASS_SCORE = new BigDecimal("80");

    /** 评分阈值：基本通过（>=60） */
    public static final BigDecimal BASIC_SCORE = new BigDecimal("60");

    /** 评分阈值：不合格（<60） */
    public static final BigDecimal FAIL_SCORE = new BigDecimal("60");

    /** 考核通过所需最低订单数 */
    public static final long MIN_ORDER_COUNT_PASS = 10L;

    /** 考核通过所需最低订单金额 */
    public static final java.math.BigDecimal MIN_ORDER_AMOUNT_PASS = new java.math.BigDecimal("1000.00");

    /** 考核通过所需最低商品数 */
    public static final long MIN_PRODUCT_COUNT_PASS = 5L;

    /** 退款率上限（超过则考核不通过） */
    public static final java.math.BigDecimal MAX_REFUND_RATE = new java.math.BigDecimal("0.20");

    /** 违规次数上限（超过则考核不通过） */
    public static final long MAX_VIOLATION_COUNT = 3L;

    /** 投诉数上限（超过则考核不通过） */
    public static final long MAX_COMPLAINT_COUNT = 5L;
}
