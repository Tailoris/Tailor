package com.tailoris.order.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("创建订单请求DTO测试")
class CreateOrderRequestTest {

    @Test
    @DisplayName("创建订单请求 - 基本属性")
    void testCreateOrderRequest_BasicProperties() {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setCartIds(Arrays.asList(1L, 2L, 3L));
        request.setAddressId(100L);
        request.setCouponId(50L);
        request.setPromotionId(200L);
        request.setRemark("请尽快发货");
        request.setRequestId("550e8400-e29b-41d4-a716-446655440000");

        assertEquals(3, request.getCartIds().size());
        assertEquals(100L, request.getAddressId());
        assertEquals(50L, request.getCouponId());
        assertEquals(200L, request.getPromotionId());
        assertEquals("请尽快发货", request.getRemark());
        assertEquals("550e8400-e29b-41d4-a716-446655440000", request.getRequestId());
    }

    @Test
    @DisplayName("创建订单请求 - 购物车ID列表")
    void testCreateOrderRequest_CartIds() {
        CreateOrderRequest request = new CreateOrderRequest();
        List<Long> cartIds = Arrays.asList(1L, 2L, 3L, 4L, 5L);
        request.setCartIds(cartIds);

        assertNotNull(request.getCartIds());
        assertEquals(5, request.getCartIds().size());
        assertTrue(request.getCartIds().contains(1L));
        assertTrue(request.getCartIds().contains(5L));
    }

    @Test
    @DisplayName("创建订单请求 - 空购物车ID列表")
    void testCreateOrderRequest_EmptyCartIds() {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setCartIds(Collections.emptyList());

        assertNotNull(request.getCartIds());
        assertTrue(request.getCartIds().isEmpty());
    }

    @Test
    @DisplayName("创建订单请求 - 可选属性为空")
    void testCreateOrderRequest_OptionalFieldsNull() {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setCartIds(Arrays.asList(1L, 2L));
        request.setRequestId("test-request-id");

        assertNotNull(request.getCartIds());
        assertNotNull(request.getRequestId());
        assertNull(request.getAddressId());
        assertNull(request.getCouponId());
        assertNull(request.getPromotionId());
        assertNull(request.getRemark());
    }

    @Test
    @DisplayName("创建订单请求 - 仅必填字段")
    void testCreateOrderRequest_RequiredFieldsOnly() {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setCartIds(Arrays.asList(1L));
        request.setRequestId("uuid-123");

        assertNotNull(request.getCartIds());
        assertFalse(request.getCartIds().isEmpty());
        assertNotNull(request.getRequestId());
        assertFalse(request.getRequestId().isEmpty());
    }

    @Test
    @DisplayName("创建订单请求 - 地址ID")
    void testCreateOrderRequest_AddressId() {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setAddressId(100L);

        assertEquals(100L, request.getAddressId());
    }

    @Test
    @DisplayName("创建订单请求 - 优惠券ID")
    void testCreateOrderRequest_CouponId() {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setCouponId(50L);

        assertEquals(50L, request.getCouponId());
    }

    @Test
    @DisplayName("创建订单请求 - 促销活动ID")
    void testCreateOrderRequest_PromotionId() {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setPromotionId(200L);

        assertEquals(200L, request.getPromotionId());
    }

    @Test
    @DisplayName("创建订单请求 - 备注信息")
    void testCreateOrderRequest_Remark() {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setRemark("请使用顺丰快递，谢谢！");

        assertEquals("请使用顺丰快递，谢谢！", request.getRemark());
    }

    @Test
    @DisplayName("创建订单请求 - 幂等请求ID")
    void testCreateOrderRequest_RequestId() {
        CreateOrderRequest request = new CreateOrderRequest();
        String requestId = "550e8400-e29b-41d4-a716-446655440000";
        request.setRequestId(requestId);

        assertEquals(requestId, request.getRequestId());
        assertEquals(36, request.getRequestId().length());
    }

    @Test
    @DisplayName("创建订单请求 - 多个购物车ID")
    void testCreateOrderRequest_MultipleCartIds() {
        CreateOrderRequest request = new CreateOrderRequest();
        List<Long> cartIds = Arrays.asList(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L);
        request.setCartIds(cartIds);

        assertEquals(10, request.getCartIds().size());
        assertEquals(1L, request.getCartIds().get(0));
        assertEquals(10L, request.getCartIds().get(9));
    }
}
