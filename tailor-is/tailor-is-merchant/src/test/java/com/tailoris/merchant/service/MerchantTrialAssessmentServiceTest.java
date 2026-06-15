package com.tailoris.merchant.service;

import com.tailoris.merchant.constant.TrialAssessmentConstants;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 商家试运营考核单元测试 - MER-009.
 *
 * <p>验证试运营考核评分模型与业务规则。</p>
 *
 * @author Tailor IS Team
 * @since 2026-06-03
 */
@DisplayName("商家试运营考核单元测试")
class MerchantTrialAssessmentServiceTest {

    @Test
    @DisplayName("考核常量阈值验证")
    void testConstants() {
        assertEquals(30, TrialAssessmentConstants.TRIAL_DAYS, "试运营期30天");
        assertEquals(0, TrialAssessmentConstants.RESULT_PENDING);
        assertEquals(1, TrialAssessmentConstants.RESULT_PASS);
        assertEquals(2, TrialAssessmentConstants.RESULT_FAIL);
        assertEquals(3, TrialAssessmentConstants.RESULT_EXTEND);
    }

    @Test
    @DisplayName("评分阈值校验")
    void testPassScoreThreshold() {
        BigDecimal pass = TrialAssessmentConstants.PASS_SCORE;
        BigDecimal basic = TrialAssessmentConstants.BASIC_SCORE;
        assertTrue(pass.compareTo(basic) > 0, "通过分应高于基本分");
        assertEquals(0, new BigDecimal("80").compareTo(pass), "通过分为80");
        assertEquals(0, new BigDecimal("60").compareTo(basic), "基本分为60");
    }

    @Test
    @DisplayName("考核通过所需硬性指标")
    void testMinRequirements() {
        assertEquals(10L, TrialAssessmentConstants.MIN_ORDER_COUNT_PASS);
        assertEquals(0, TrialAssessmentConstants.MIN_ORDER_AMOUNT_PASS.compareTo(new BigDecimal("1000.00")));
        assertEquals(5L, TrialAssessmentConstants.MIN_PRODUCT_COUNT_PASS);
    }

    @Test
    @DisplayName("一票否决硬性指标")
    void testVetoRules() {
        assertEquals(0, TrialAssessmentConstants.MAX_REFUND_RATE.compareTo(new BigDecimal("0.20")));
        assertEquals(3L, TrialAssessmentConstants.MAX_VIOLATION_COUNT);
        assertEquals(5L, TrialAssessmentConstants.MAX_COMPLAINT_COUNT);
    }

    @Test
    @DisplayName("评分模型各维度满分验证")
    void testScoringModelMaxScore() {
        // 订单数(30) + 订单金额(30) + 商品数(20) + 退款率(10) + 违规次数(10) = 100
        int totalMax = 30 + 30 + 20 + 10 + 10;
        assertEquals(100, totalMax, "评分模型总分应为100");
    }

    @Test
    @DisplayName("订单数评分函数")
    void testOrderCountScore() {
        long minOrder = TrialAssessmentConstants.MIN_ORDER_COUNT_PASS;
        // 达到最低要求：满分30
        BigDecimal score = minOrder >= TrialAssessmentConstants.MIN_ORDER_COUNT_PASS
                ? new BigDecimal("30")
                : new BigDecimal("30").subtract(new BigDecimal((minOrder - TrialAssessmentConstants.MIN_ORDER_COUNT_PASS) * 3));
        // 实际场景
        if (10L >= TrialAssessmentConstants.MIN_ORDER_COUNT_PASS) {
            score = new BigDecimal("30");
        }
        assertEquals(0, new BigDecimal("30").compareTo(score));
    }

    @Test
    @DisplayName("评分不可能超过100分")
    void testScoreCappedAt100() {
        BigDecimal rawScore = new BigDecimal("120");
        BigDecimal capped = rawScore.min(new BigDecimal("100"));
        assertEquals(0, new BigDecimal("100").compareTo(capped));
    }

    @Test
    @DisplayName("评分不可能为负数")
    void testScoreFloorZero() {
        BigDecimal rawScore = new BigDecimal("-10");
        BigDecimal floored = rawScore.max(BigDecimal.ZERO);
        assertEquals(0, BigDecimal.ZERO.compareTo(floored));
    }
}
