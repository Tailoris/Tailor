package com.tailoris.marketing.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("用户优惠券实体测试")
class UserCouponTest {

    @Test
    @DisplayName("创建用户优惠券 - 基本属性")
    void testUserCoupon_BasicProperties() {
        UserCoupon coupon = new UserCoupon();
        coupon.setId(1L);
        coupon.setUserId(100L);
        coupon.setCouponId(50L);
        coupon.setCouponCode("CPN202606160001");
        coupon.setStatus(0);

        assertEquals(1L, coupon.getId());
        assertEquals(100L, coupon.getUserId());
        assertEquals(50L, coupon.getCouponId());
        assertEquals("CPN202606160001", coupon.getCouponCode());
        assertEquals(0, coupon.getStatus());
    }

    @Test
    @DisplayName("创建用户优惠券 - 使用时间")
    void testUserCoupon_UsedTime() {
        UserCoupon coupon = new UserCoupon();
        LocalDateTime now = LocalDateTime.now();
        coupon.setUsedTime(now);

        assertEquals(now, coupon.getUsedTime());
    }

    @Test
    @DisplayName("创建用户优惠券 - 订单信息")
    void testUserCoupon_OrderInfo() {
        UserCoupon coupon = new UserCoupon();
        coupon.setOrderId(200L);
        coupon.setOrderNo("ORD202606160001");

        assertEquals(200L, coupon.getOrderId());
        assertEquals("ORD202606160001", coupon.getOrderNo());
    }

    @Test
    @DisplayName("创建用户优惠券 - 有效期")
    void testUserCoupon_ValidTime() {
        UserCoupon coupon = new UserCoupon();
        LocalDateTime startTime = LocalDateTime.now();
        LocalDateTime endTime = startTime.plusDays(30);
        coupon.setValidStartTime(startTime);
        coupon.setValidEndTime(endTime);

        assertEquals(startTime, coupon.getValidStartTime());
        assertEquals(endTime, coupon.getValidEndTime());
        assertTrue(coupon.getValidEndTime().isAfter(coupon.getValidStartTime()));
    }

    @Test
    @DisplayName("创建用户优惠券 - 不同状态")
    void testUserCoupon_DifferentStatus() {
        UserCoupon coupon = new UserCoupon();

        // 未使用
        coupon.setStatus(0);
        assertEquals(0, coupon.getStatus());

        // 已使用
        coupon.setStatus(1);
        assertEquals(1, coupon.getStatus());

        // 已过期
        coupon.setStatus(2);
        assertEquals(2, coupon.getStatus());

        // 已作废
        coupon.setStatus(3);
        assertEquals(3, coupon.getStatus());
    }

    @Test
    @DisplayName("创建用户优惠券 - 优惠券编码格式")
    void testUserCoupon_CouponCodeFormat() {
        UserCoupon coupon = new UserCoupon();
        coupon.setCouponCode("CPN202606160001");

        assertNotNull(coupon.getCouponCode());
        assertTrue(coupon.getCouponCode().startsWith("CPN"));
        assertEquals(15, coupon.getCouponCode().length());
    }

    @Test
    @DisplayName("创建用户优惠券 - 有效期检查")
    void testUserCoupon_ValidPeriodCheck() {
        UserCoupon coupon = new UserCoupon();
        LocalDateTime now = LocalDateTime.now();
        coupon.setValidStartTime(now.minusDays(1));
        coupon.setValidEndTime(now.plusDays(1));

        assertTrue(now.isAfter(coupon.getValidStartTime()));
        assertTrue(now.isBefore(coupon.getValidEndTime()));
    }

    @Test
    @DisplayName("创建用户优惠券 - 已过期检查")
    void testUserCoupon_ExpiredCheck() {
        UserCoupon coupon = new UserCoupon();
        LocalDateTime now = LocalDateTime.now();
        coupon.setValidStartTime(now.minusDays(30));
        coupon.setValidEndTime(now.minusDays(1));

        assertTrue(now.isAfter(coupon.getValidEndTime()));
    }

    @Test
    @DisplayName("创建用户优惠券 - 未开始检查")
    void testUserCoupon_NotStartedCheck() {
        UserCoupon coupon = new UserCoupon();
        LocalDateTime now = LocalDateTime.now();
        coupon.setValidStartTime(now.plusDays(1));
        coupon.setValidEndTime(now.plusDays(30));

        assertTrue(now.isBefore(coupon.getValidStartTime()));
    }

    @Test
    @DisplayName("创建用户优惠券 - 使用记录")
    void testUserCoupon_UsageRecord() {
        UserCoupon coupon = new UserCoupon();
        coupon.setStatus(1);
        coupon.setUsedTime(LocalDateTime.now());
        coupon.setOrderId(200L);
        coupon.setOrderNo("ORD202606160001");

        assertEquals(1, coupon.getStatus());
        assertNotNull(coupon.getUsedTime());
        assertEquals(200L, coupon.getOrderId());
        assertEquals("ORD202606160001", coupon.getOrderNo());
    }

    @Test
    @DisplayName("创建用户优惠券 - 未使用状态")
    void testUserCoupon_UnusedStatus() {
        UserCoupon coupon = new UserCoupon();
        coupon.setStatus(0);

        assertEquals(0, coupon.getStatus());
        assertNull(coupon.getUsedTime());
        assertNull(coupon.getOrderId());
        assertNull(coupon.getOrderNo());
    }
}
