package com.tailoris.marketing.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tailoris.marketing.entity.CouponTemplate;
import com.tailoris.marketing.entity.UserCoupon;
import com.tailoris.marketing.mapper.CouponTemplateMapper;
import com.tailoris.marketing.mapper.UserCouponMapper;
import com.tailoris.marketing.service.MktOrderMatchService.OrderDiscountPlan;
import com.tailoris.marketing.service.MktOrderMatchService.OrderItemInput;
import com.tailoris.marketing.service.MktPromotionService;
import com.tailoris.marketing.service.MktPromotionService.PromotionDiscountResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("MktOrderMatchServiceImpl 单元测试")
@ExtendWith(MockitoExtension.class)
class MktOrderMatchServiceImplTest {

    @Mock
    private UserCouponMapper userCouponMapper;

    @Mock
    private CouponTemplateMapper couponTemplateMapper;

    @Mock
    private MktPromotionService promotionService;

    @InjectMocks
    private MktOrderMatchServiceImpl orderMatchService;

    private CouponTemplate fixedCoupon;
    private CouponTemplate percentCoupon;
    private UserCoupon userCoupon;

    @BeforeEach
    void setUp() {
        // 固定金额优惠券
        fixedCoupon = new CouponTemplate();
        fixedCoupon.setId(1L);
        fixedCoupon.setName("满50减10");
        fixedCoupon.setDiscountType(1);
        fixedCoupon.setDiscountValue(new BigDecimal("10"));
        fixedCoupon.setMinAmount(new BigDecimal("50"));
        fixedCoupon.setStatus(1);

        // 百分比折扣优惠券
        percentCoupon = new CouponTemplate();
        percentCoupon.setId(2L);
        percentCoupon.setName("8折优惠");
        percentCoupon.setDiscountType(2);
        percentCoupon.setDiscountValue(new BigDecimal("80"));
        percentCoupon.setMinAmount(new BigDecimal("100"));
        percentCoupon.setMaxDiscount(new BigDecimal("50"));
        percentCoupon.setStatus(1);

        // 用户优惠券
        userCoupon = new UserCoupon();
        userCoupon.setId(1L);
        userCoupon.setUserId(1L);
        userCoupon.setCouponId(1L);
        userCoupon.setStatus(0);
        userCoupon.setValidEndTime(LocalDateTime.now().plusDays(7));
    }

    @Test
    @DisplayName("计算最优优惠：订单金额为null返回0")
    void testCalculateOptimal_NullAmount() {
        OrderDiscountPlan plan = orderMatchService.calculateOptimal(1L, Collections.emptyList(), null);
        assertNotNull(plan);
        assertEquals(0, BigDecimal.ZERO.compareTo(plan.getTotalDiscount()));
    }

    @Test
    @DisplayName("计算最优优惠：订单金额为0返回0")
    void testCalculateOptimal_ZeroAmount() {
        OrderDiscountPlan plan = orderMatchService.calculateOptimal(1L, Collections.emptyList(), BigDecimal.ZERO);
        assertNotNull(plan);
        assertEquals(0, BigDecimal.ZERO.compareTo(plan.getTotalDiscount()));
    }

    @Test
    @DisplayName("计算最优优惠：订单金额为负数返回0")
    void testCalculateOptimal_NegativeAmount() {
        OrderDiscountPlan plan = orderMatchService.calculateOptimal(1L, Collections.emptyList(), new BigDecimal("-10"));
        assertNotNull(plan);
        assertEquals(0, BigDecimal.ZERO.compareTo(plan.getTotalDiscount()));
    }

    @Test
    @DisplayName("计算最优优惠：无商品列表时仅计算优惠券")
    void testCalculateOptimal_NullItems() {
        when(userCouponMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());

        OrderDiscountPlan plan = orderMatchService.calculateOptimal(1L, null, new BigDecimal("100"));

        assertNotNull(plan);
        assertEquals(0, BigDecimal.ZERO.compareTo(plan.getTotalDiscount()));
    }

    @Test
    @DisplayName("计算最优优惠：空商品列表时仅计算优惠券")
    void testCalculateOptimal_EmptyItems() {
        when(userCouponMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());

        OrderDiscountPlan plan = orderMatchService.calculateOptimal(1L, Collections.emptyList(), new BigDecimal("100"));

        assertNotNull(plan);
        assertEquals(0, BigDecimal.ZERO.compareTo(plan.getTotalDiscount()));
    }

    @Test
    @DisplayName("计算最优优惠：商品有活动标记")
    void testCalculateOptimal_WithActivePromotion() {
        OrderItemInput item = new OrderItemInput();
        item.setProductId(1L);
        item.setShopId(1L);
        item.setPrice(new BigDecimal("50"));
        item.setQuantity(2);
        item.setActivePromotionType(2); // 拼团
        item.setActivePromotionId(100L);

        when(userCouponMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());
        when(promotionService.calculateOptimalDiscount(anyLong(), any(BigDecimal.class), anyInt())).thenReturn(null);

        OrderDiscountPlan plan = orderMatchService.calculateOptimal(1L, Arrays.asList(item), new BigDecimal("100"));

        assertNotNull(plan);
        assertNotNull(plan.getAppliedActivities());
        assertTrue(plan.getAppliedActivities().contains("活动2#100"));
    }

    @Test
    @DisplayName("计算最优优惠：满减活动生效")
    void testCalculateOptimal_WithPromotionDiscount() {
        OrderItemInput item = new OrderItemInput();
        item.setProductId(1L);
        item.setShopId(1L);
        item.setPrice(new BigDecimal("100"));
        item.setQuantity(1);

        PromotionDiscountResult promoResult = new PromotionDiscountResult();
        promoResult.setPromotionId(1L);
        promoResult.setPromotionName("满100减20");
        promoResult.setDiscountAmount(new BigDecimal("20"));

        when(userCouponMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());
        when(promotionService.calculateOptimalDiscount(eq(1L), any(BigDecimal.class), anyInt())).thenReturn(promoResult);

        OrderDiscountPlan plan = orderMatchService.calculateOptimal(1L, Arrays.asList(item), new BigDecimal("100"));

        assertNotNull(plan);
        assertEquals(1L, plan.getPromotionId());
        assertEquals(0, new BigDecimal("20").compareTo(plan.getPromotionDiscount()));
        assertTrue(plan.getAppliedActivities().contains("满减#满100减20"));
    }

    @Test
    @DisplayName("计算最优优惠：固定金额优惠券生效")
    void testCalculateOptimal_WithFixedCoupon() {
        OrderItemInput item = new OrderItemInput();
        item.setProductId(1L);
        item.setShopId(1L);
        item.setPrice(new BigDecimal("100"));
        item.setQuantity(1);

        when(userCouponMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Arrays.asList(userCoupon));
        when(couponTemplateMapper.selectById(1L)).thenReturn(fixedCoupon);
        when(promotionService.calculateOptimalDiscount(anyLong(), any(BigDecimal.class), anyInt())).thenReturn(null);

        OrderDiscountPlan plan = orderMatchService.calculateOptimal(1L, Arrays.asList(item), new BigDecimal("100"));

        assertNotNull(plan);
        assertEquals(1L, plan.getUserCouponId());
        assertEquals(1L, plan.getCouponTemplateId());
        assertEquals(0, new BigDecimal("10").compareTo(plan.getCouponDiscount()));
    }

    @Test
    @DisplayName("计算最优优惠：百分比折扣优惠券生效")
    void testCalculateOptimal_WithPercentCoupon() {
        UserCoupon percentUserCoupon = new UserCoupon();
        percentUserCoupon.setId(2L);
        percentUserCoupon.setUserId(1L);
        percentUserCoupon.setCouponId(2L);
        percentUserCoupon.setStatus(0);
        percentUserCoupon.setValidEndTime(LocalDateTime.now().plusDays(7));

        when(userCouponMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Arrays.asList(percentUserCoupon));
        when(couponTemplateMapper.selectById(2L)).thenReturn(percentCoupon);

        OrderDiscountPlan plan = orderMatchService.calculateOptimal(1L, Collections.emptyList(), new BigDecimal("200"));

        assertNotNull(plan);
        assertEquals(2L, plan.getUserCouponId());
        // 200 * (1 - 0.8) = 40, 但不超过maxDiscount=50
        assertEquals(0, new BigDecimal("40.0000").compareTo(plan.getCouponDiscount()));
    }

    @Test
    @DisplayName("计算最优优惠：百分比折扣超过最大优惠时封顶")
    void testCalculateOptimal_PercentCouponWithMaxDiscount() {
        UserCoupon percentUserCoupon = new UserCoupon();
        percentUserCoupon.setId(2L);
        percentUserCoupon.setUserId(1L);
        percentUserCoupon.setCouponId(2L);
        percentUserCoupon.setStatus(0);
        percentUserCoupon.setValidEndTime(LocalDateTime.now().plusDays(7));

        when(userCouponMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Arrays.asList(percentUserCoupon));
        when(couponTemplateMapper.selectById(2L)).thenReturn(percentCoupon);

        // 1000 * 0.2 = 200, 但maxDiscount=50
        OrderDiscountPlan plan = orderMatchService.calculateOptimal(1L, Collections.emptyList(), new BigDecimal("1000"));

        assertNotNull(plan);
        assertEquals(0, new BigDecimal("50").compareTo(plan.getCouponDiscount()));
    }

    @Test
    @DisplayName("计算最优优惠：优惠总额不超过订单金额")
    void testCalculateOptimal_TotalDiscountCapped() {
        UserCoupon percentUserCoupon = new UserCoupon();
        percentUserCoupon.setId(2L);
        percentUserCoupon.setUserId(1L);
        percentUserCoupon.setCouponId(2L);
        percentUserCoupon.setStatus(0);
        percentUserCoupon.setValidEndTime(LocalDateTime.now().plusDays(7));

        PromotionDiscountResult promoResult = new PromotionDiscountResult();
        promoResult.setPromotionId(1L);
        promoResult.setPromotionName("满50减40");
        promoResult.setDiscountAmount(new BigDecimal("40"));

        when(userCouponMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Arrays.asList(percentUserCoupon));
        when(couponTemplateMapper.selectById(2L)).thenReturn(percentCoupon);
        when(promotionService.calculateOptimalDiscount(eq(1L), any(BigDecimal.class), anyInt())).thenReturn(promoResult);

        OrderItemInput item = new OrderItemInput();
        item.setProductId(1L);
        item.setShopId(1L);
        item.setPrice(new BigDecimal("50"));
        item.setQuantity(1);

        // 订单金额50，满减40 + 优惠券(50-40)*0.2=2，共42 < 50
        OrderDiscountPlan plan = orderMatchService.calculateOptimal(1L, Arrays.asList(item), new BigDecimal("50"));

        assertNotNull(plan);
        assertTrue(plan.getTotalDiscount().compareTo(new BigDecimal("50")) <= 0);
    }

    @Test
    @DisplayName("计算最优优惠：userId为null时跳过优惠券")
    void testCalculateOptimal_NullUserId() {
        OrderItemInput item = new OrderItemInput();
        item.setProductId(1L);
        item.setShopId(1L);
        item.setPrice(new BigDecimal("100"));
        item.setQuantity(1);

        when(promotionService.calculateOptimalDiscount(anyLong(), any(BigDecimal.class), anyInt())).thenReturn(null);

        OrderDiscountPlan plan = orderMatchService.calculateOptimal(null, Arrays.asList(item), new BigDecimal("100"));

        assertNotNull(plan);
        assertNull(plan.getUserCouponId());
    }

    @Test
    @DisplayName("计算最优优惠：优惠券模板不存在")
    void testCalculateOptimal_CouponTemplateNotFound() {
        when(userCouponMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Arrays.asList(userCoupon));
        when(couponTemplateMapper.selectById(1L)).thenReturn(null);

        OrderDiscountPlan plan = orderMatchService.calculateOptimal(1L, Collections.emptyList(), new BigDecimal("100"));

        assertNotNull(plan);
        assertNull(plan.getUserCouponId());
    }

    @Test
    @DisplayName("计算最优优惠：优惠券不满足最低消费")
    void testCalculateOptimal_CouponMinAmountNotMet() {
        when(userCouponMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Arrays.asList(userCoupon));
        when(couponTemplateMapper.selectById(1L)).thenReturn(fixedCoupon);

        // 订单金额30，但优惠券要求最低50
        OrderDiscountPlan plan = orderMatchService.calculateOptimal(1L, Collections.emptyList(), new BigDecimal("30"));

        assertNotNull(plan);
        assertNull(plan.getUserCouponId());
    }

    @Test
    @DisplayName("仅计算优惠券：userId为null返回0")
    void testCalculateCouponOnly_NullUserId() {
        OrderDiscountPlan plan = orderMatchService.calculateCouponOnly(null, new BigDecimal("100"));
        assertNotNull(plan);
        assertEquals(0, BigDecimal.ZERO.compareTo(plan.getTotalDiscount()));
    }

    @Test
    @DisplayName("仅计算优惠券：订单金额为null返回0")
    void testCalculateCouponOnly_NullAmount() {
        OrderDiscountPlan plan = orderMatchService.calculateCouponOnly(1L, null);
        assertNotNull(plan);
        assertEquals(0, BigDecimal.ZERO.compareTo(plan.getTotalDiscount()));
    }

    @Test
    @DisplayName("仅计算优惠券：订单金额为0返回0")
    void testCalculateCouponOnly_ZeroAmount() {
        OrderDiscountPlan plan = orderMatchService.calculateCouponOnly(1L, BigDecimal.ZERO);
        assertNotNull(plan);
        assertEquals(0, BigDecimal.ZERO.compareTo(plan.getTotalDiscount()));
    }

    @Test
    @DisplayName("仅计算优惠券：无可用优惠券返回0")
    void testCalculateCouponOnly_NoCoupon() {
        when(userCouponMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());

        OrderDiscountPlan plan = orderMatchService.calculateCouponOnly(1L, new BigDecimal("100"));

        assertNotNull(plan);
        assertEquals(0, BigDecimal.ZERO.compareTo(plan.getTotalDiscount()));
    }

    @Test
    @DisplayName("仅计算优惠券：优惠券模板不存在返回0")
    void testCalculateCouponOnly_TemplateNotFound() {
        when(userCouponMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Arrays.asList(userCoupon));
        when(couponTemplateMapper.selectById(1L)).thenReturn(null);

        OrderDiscountPlan plan = orderMatchService.calculateCouponOnly(1L, new BigDecimal("100"));

        assertNotNull(plan);
        assertEquals(0, BigDecimal.ZERO.compareTo(plan.getTotalDiscount()));
    }

    @Test
    @DisplayName("仅计算优惠券：成功计算固定金额优惠")
    void testCalculateCouponOnly_FixedDiscount() {
        when(userCouponMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Arrays.asList(userCoupon));
        when(couponTemplateMapper.selectById(1L)).thenReturn(fixedCoupon);

        OrderDiscountPlan plan = orderMatchService.calculateCouponOnly(1L, new BigDecimal("100"));

        assertNotNull(plan);
        assertEquals(1L, plan.getUserCouponId());
        assertEquals(1L, plan.getCouponTemplateId());
        assertEquals(0, new BigDecimal("10").compareTo(plan.getTotalDiscount()));
        assertTrue(plan.getAppliedActivities().contains("优惠券#满50减10"));
    }

    @Test
    @DisplayName("仅计算优惠券：固定金额超过订单金额时封顶")
    void testCalculateCouponOnly_FixedDiscountCapped() {
        CouponTemplate highValueCoupon = new CouponTemplate();
        highValueCoupon.setId(10L);
        highValueCoupon.setName("大额固定券");
        highValueCoupon.setDiscountType(1);
        highValueCoupon.setDiscountValue(new BigDecimal("10"));
        highValueCoupon.setMinAmount(BigDecimal.ZERO);

        UserCoupon uc = new UserCoupon();
        uc.setId(10L);
        uc.setUserId(1L);
        uc.setCouponId(10L);
        uc.setStatus(0);
        uc.setValidEndTime(LocalDateTime.now().plusDays(7));

        when(userCouponMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Arrays.asList(uc));
        when(couponTemplateMapper.selectById(10L)).thenReturn(highValueCoupon);

        // 订单金额8，优惠券面额10，实际只能优惠8
        OrderDiscountPlan plan = orderMatchService.calculateCouponOnly(1L, new BigDecimal("8"));

        assertNotNull(plan);
        assertEquals(0, new BigDecimal("8").compareTo(plan.getTotalDiscount()));
    }

    @Test
    @DisplayName("选择最优优惠券：多张券选最大优惠")
    void testSelectBestCoupon_MultipleCoupons() {
        UserCoupon coupon1 = new UserCoupon();
        coupon1.setId(1L);
        coupon1.setUserId(1L);
        coupon1.setCouponId(1L);
        coupon1.setStatus(0);
        coupon1.setValidEndTime(LocalDateTime.now().plusDays(7));

        UserCoupon coupon2 = new UserCoupon();
        coupon2.setId(2L);
        coupon2.setUserId(1L);
        coupon2.setCouponId(2L);
        coupon2.setStatus(0);
        coupon2.setValidEndTime(LocalDateTime.now().plusDays(7));

        CouponTemplate smallCoupon = new CouponTemplate();
        smallCoupon.setId(1L);
        smallCoupon.setName("小额券");
        smallCoupon.setDiscountType(1);
        smallCoupon.setDiscountValue(new BigDecimal("5"));
        smallCoupon.setMinAmount(new BigDecimal("50"));

        CouponTemplate largeCoupon = new CouponTemplate();
        largeCoupon.setId(2L);
        largeCoupon.setName("大额券");
        largeCoupon.setDiscountType(1);
        largeCoupon.setDiscountValue(new BigDecimal("20"));
        largeCoupon.setMinAmount(new BigDecimal("50"));

        when(userCouponMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Arrays.asList(coupon1, coupon2));
        when(couponTemplateMapper.selectById(1L)).thenReturn(smallCoupon);
        when(couponTemplateMapper.selectById(2L)).thenReturn(largeCoupon);

        OrderDiscountPlan plan = orderMatchService.calculateOptimal(1L, Collections.emptyList(), new BigDecimal("100"));

        assertNotNull(plan);
        assertEquals(2L, plan.getUserCouponId());
        assertEquals(0, new BigDecimal("20").compareTo(plan.getCouponDiscount()));
    }

    @Test
    @DisplayName("选择最优优惠券：模板minAmount为null时跳过")
    void testSelectBestCoupon_NullMinAmount() {
        CouponTemplate noMinCoupon = new CouponTemplate();
        noMinCoupon.setId(1L);
        noMinCoupon.setName("无门槛券");
        noMinCoupon.setDiscountType(1);
        noMinCoupon.setDiscountValue(new BigDecimal("10"));
        noMinCoupon.setMinAmount(null);

        when(userCouponMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Arrays.asList(userCoupon));
        when(couponTemplateMapper.selectById(1L)).thenReturn(noMinCoupon);

        OrderDiscountPlan plan = orderMatchService.calculateOptimal(1L, Collections.emptyList(), new BigDecimal("100"));

        assertNotNull(plan);
        assertNull(plan.getUserCouponId());
    }
}
