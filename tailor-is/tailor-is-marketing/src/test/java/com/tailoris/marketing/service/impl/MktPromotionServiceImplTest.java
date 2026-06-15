package com.tailoris.marketing.service.impl;

import com.tailoris.common.exception.BusinessException;
import com.tailoris.marketing.entity.MktPromotionRule;
import com.tailoris.marketing.entity.MktPromotionStep;
import com.tailoris.marketing.mapper.MktPromotionRuleMapper;
import com.tailoris.marketing.mapper.MktPromotionStepMapper;
import com.tailoris.marketing.service.MktPromotionService.PromotionDiscountResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@DisplayName("MktPromotionServiceImpl 单元测试")
@ExtendWith(MockitoExtension.class)
class MktPromotionServiceImplTest {

    @Mock
    private MktPromotionRuleMapper ruleMapper;

    @Mock
    private MktPromotionStepMapper stepMapper;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @InjectMocks
    private MktPromotionServiceImpl promotionService;

    private MktPromotionRule rule;
    private MktPromotionStep step1;
    private MktPromotionStep step2;

    @BeforeEach
    void setUp() {
        rule = new MktPromotionRule();
        rule.setId(1L);
        rule.setPromotionName("满减活动");
        rule.setPromotionType(4);
        rule.setShopId(1L);
        rule.setStatus(1);
        rule.setStartTime(LocalDateTime.now().minusDays(1));
        rule.setEndTime(LocalDateTime.now().plusDays(7));
        rule.setThresholdType(1);
        rule.setPriority(10);

        step1 = new MktPromotionStep();
        step1.setId(1L);
        step1.setPromotionId(1L);
        step1.setThresholdValue(new BigDecimal("100"));
        step1.setDiscountType(1);
        step1.setDiscountValue(new BigDecimal("10"));
        step1.setSortOrder(1);

        step2 = new MktPromotionStep();
        step2.setId(2L);
        step2.setPromotionId(1L);
        step2.setThresholdValue(new BigDecimal("200"));
        step2.setDiscountType(1);
        step2.setDiscountValue(new BigDecimal("30"));
        step2.setSortOrder(2);
    }

    @Test
    @DisplayName("创建促销：阶梯为空应抛异常")
    void testCreatePromotion_EmptySteps() {
        assertThrows(BusinessException.class,
                () -> promotionService.createPromotion(rule, Collections.emptyList()));
    }

    @Test
    @DisplayName("创建促销：结束时间早于开始时间应抛异常")
    void testCreatePromotion_InvalidTime() {
        rule.setStartTime(LocalDateTime.now().plusDays(2));
        rule.setEndTime(LocalDateTime.now().plusDays(1));
        assertThrows(BusinessException.class,
                () -> promotionService.createPromotion(rule, Arrays.asList(step1)));
    }

    @Test
    @DisplayName("计算最优优惠：满足200元减30元阶梯")
    void testCalculateOptimal_HighestDiscount() {
        when(ruleMapper.selectActiveRules(any(), any())).thenReturn(Arrays.asList(rule));
        when(stepMapper.selectByPromotionId(1L)).thenReturn(Arrays.asList(step1, step2));
        PromotionDiscountResult result = promotionService.calculateOptimalDiscount(
                1L, new BigDecimal("250"), 1);
        assertNotNull(result);
        assertEquals(0, new BigDecimal("30").compareTo(result.getDiscountAmount()));
    }

    @Test
    @DisplayName("计算最优优惠：仅满足100元减10元阶梯")
    void testCalculateOptimal_LowerDiscount() {
        when(ruleMapper.selectActiveRules(any(), any())).thenReturn(Arrays.asList(rule));
        when(stepMapper.selectByPromotionId(1L)).thenReturn(Arrays.asList(step1, step2));
        PromotionDiscountResult result = promotionService.calculateOptimalDiscount(
                1L, new BigDecimal("120"), 1);
        assertNotNull(result);
        assertEquals(0, new BigDecimal("10").compareTo(result.getDiscountAmount()));
    }

    @Test
    @DisplayName("计算最优优惠：订单金额低于门槛无优惠")
    void testCalculateOptimal_NoMatch() {
        when(ruleMapper.selectActiveRules(any(), any())).thenReturn(Arrays.asList(rule));
        when(stepMapper.selectByPromotionId(1L)).thenReturn(Arrays.asList(step1, step2));
        PromotionDiscountResult result = promotionService.calculateOptimalDiscount(
                1L, new BigDecimal("50"), 1);
        assertNull(result);
    }

    @Test
    @DisplayName("计算最优优惠：百分比折扣阶梯")
    void testCalculateOptimal_PercentageDiscount() {
        MktPromotionStep stepPercent = new MktPromotionStep();
        stepPercent.setId(3L);
        stepPercent.setPromotionId(1L);
        stepPercent.setThresholdValue(new BigDecimal("100"));
        stepPercent.setDiscountType(2); // 百分比
        stepPercent.setDiscountValue(new BigDecimal("80")); // 8折
        stepPercent.setMaxDiscount(new BigDecimal("30"));

        when(ruleMapper.selectActiveRules(any(), any())).thenReturn(Arrays.asList(rule));
        when(stepMapper.selectByPromotionId(1L)).thenReturn(Arrays.asList(stepPercent));
        PromotionDiscountResult result = promotionService.calculateOptimalDiscount(
                1L, new BigDecimal("200"), 1);
        assertNotNull(result);
        // 200*0.2=40, 限30
        assertEquals(0, new BigDecimal("30").compareTo(result.getDiscountAmount()));
    }
}
