package com.tailoris.order.entity;

import com.tailoris.order.constant.OrderConstants;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("订单信息实体测试")
class OrderInfoTest {

    @Test
    @DisplayName("创建订单信息 - 基本属性")
    void testOrderInfo_BasicProperties() {
        OrderInfo order = new OrderInfo();
        order.setId(1L);
        order.setOrderNo("ORD202606160001");
        order.setUserId(100L);
        order.setShopId(10L);
        order.setMerchantId(5L);
        order.setStatus(OrderConstants.ORDER_STATUS_PENDING_PAY);
        order.setTotalAmount(new BigDecimal("100.00"));
        order.setPayAmount(new BigDecimal("90.00"));

        assertEquals(1L, order.getId());
        assertEquals("ORD202606160001", order.getOrderNo());
        assertEquals(100L, order.getUserId());
        assertEquals(10L, order.getShopId());
        assertEquals(5L, order.getMerchantId());
        assertEquals(OrderConstants.ORDER_STATUS_PENDING_PAY, order.getStatus());
        assertEquals(new BigDecimal("100.00"), order.getTotalAmount());
        assertEquals(new BigDecimal("90.00"), order.getPayAmount());
    }

    @Test
    @DisplayName("创建订单信息 - 金额计算")
    void testOrderInfo_AmountCalculation() {
        OrderInfo order = new OrderInfo();
        order.setTotalAmount(new BigDecimal("100.00"));
        order.setDiscountAmount(new BigDecimal("5.00"));
        order.setCouponAmount(new BigDecimal("3.00"));
        order.setPointsAmount(new BigDecimal("2.00"));
        order.setFreightAmount(new BigDecimal("10.00"));

        BigDecimal payAmount = order.getTotalAmount()
            .subtract(order.getDiscountAmount())
            .subtract(order.getCouponAmount())
            .subtract(order.getPointsAmount())
            .add(order.getFreightAmount());

        assertEquals(new BigDecimal("100.00"), order.getTotalAmount());
        assertEquals(new BigDecimal("100.00"), payAmount);
    }

    @Test
    @DisplayName("创建订单信息 - 支付状态")
    void testOrderInfo_PayStatus() {
        OrderInfo order = new OrderInfo();
        order.setPayStatus(OrderConstants.PAY_STATUS_UNPAID);
        order.setPayType(1);

        assertEquals(OrderConstants.PAY_STATUS_UNPAID, order.getPayStatus());
        assertEquals(1, order.getPayType());
    }

    @Test
    @DisplayName("创建订单信息 - 时间属性")
    void testOrderInfo_TimeProperties() {
        OrderInfo order = new OrderInfo();
        LocalDateTime now = LocalDateTime.now();
        order.setPayTime(now);
        order.setExpireTime(now.plusMinutes(30));
        order.setShipTime(now.plusHours(2));
        order.setConfirmReceiveTime(now.plusDays(3));

        assertEquals(now, order.getPayTime());
        assertEquals(now.plusMinutes(30), order.getExpireTime());
        assertEquals(now.plusHours(2), order.getShipTime());
        assertEquals(now.plusDays(3), order.getConfirmReceiveTime());
    }

    @Test
    @DisplayName("创建订单信息 - 取消信息")
    void testOrderInfo_CancelInfo() {
        OrderInfo order = new OrderInfo();
        order.setStatus(OrderConstants.ORDER_STATUS_CANCELLED);
        order.setCancelReason("用户主动取消");
        order.setCancelTime(LocalDateTime.now());

        assertEquals(OrderConstants.ORDER_STATUS_CANCELLED, order.getStatus());
        assertEquals("用户主动取消", order.getCancelReason());
        assertNotNull(order.getCancelTime());
    }

    @Test
    @DisplayName("创建订单信息 - 地址快照")
    void testOrderInfo_AddressSnapshot() {
        OrderInfo order = new OrderInfo();
        order.setAddressSnapshot("{\"name\":\"张三\",\"phone\":\"13800138000\",\"address\":\"北京市朝阳区\"}");

        assertNotNull(order.getAddressSnapshot());
        assertTrue(order.getAddressSnapshot().contains("张三"));
    }

    @Test
    @DisplayName("创建订单信息 - 备注")
    void testOrderInfo_Remarks() {
        OrderInfo order = new OrderInfo();
        order.setRemark("请尽快发货");
        order.setSellerRemark("已备注");

        assertEquals("请尽快发货", order.getRemark());
        assertEquals("已备注", order.getSellerRemark());
    }

    @Test
    @DisplayName("创建订单信息 - 发票信息")
    void testOrderInfo_InvoiceInfo() {
        OrderInfo order = new OrderInfo();
        order.setInvoiceType(1);
        order.setInvoiceContent("个人");

        assertEquals(1, order.getInvoiceType());
        assertEquals("个人", order.getInvoiceContent());
    }

    @Test
    @DisplayName("创建订单信息 - 优惠券和积分")
    void testOrderInfo_CouponAndPoints() {
        OrderInfo order = new OrderInfo();
        order.setCouponId(100L);
        order.setCouponAmount(new BigDecimal("10.00"));
        order.setPointsUsed(500);
        order.setPointsAmount(new BigDecimal("5.00"));

        assertEquals(100L, order.getCouponId());
        assertEquals(new BigDecimal("10.00"), order.getCouponAmount());
        assertEquals(500, order.getPointsUsed());
        assertEquals(new BigDecimal("5.00"), order.getPointsAmount());
    }

    @Test
    @DisplayName("创建订单信息 - 物流信息")
    void testOrderInfo_LogisticsInfo() {
        OrderInfo order = new OrderInfo();
        order.setLogisticsNo("SF1234567890");
        order.setShipTime(LocalDateTime.now());

        assertEquals("SF1234567890", order.getLogisticsNo());
        assertNotNull(order.getShipTime());
    }

    @Test
    @DisplayName("创建订单信息 - 订单商品列表")
    void testOrderInfo_OrderItems() {
        OrderInfo order = new OrderInfo();
        List<OrderItem> items = new ArrayList<>();

        OrderItem item1 = new OrderItem();
        item1.setId(1L);
        item1.setProductId(100L);
        item1.setProductName("商品A");
        item1.setQuantity(2);
        item1.setPrice(new BigDecimal("50.00"));
        items.add(item1);

        OrderItem item2 = new OrderItem();
        item2.setId(2L);
        item2.setProductId(200L);
        item2.setProductName("商品B");
        item2.setQuantity(1);
        item2.setPrice(new BigDecimal("30.00"));
        items.add(item2);

        order.setOrderItems(items);

        assertNotNull(order.getOrderItems());
        assertEquals(2, order.getOrderItems().size());
        assertEquals("商品A", order.getOrderItems().get(0).getProductName());
        assertEquals(2, order.getOrderItems().get(0).getQuantity());
    }

    @Test
    @DisplayName("创建订单信息 - 物流详情")
    void testOrderInfo_Logistics() {
        OrderInfo order = new OrderInfo();
        OrderLogistics logistics = new OrderLogistics();
        logistics.setId(1L);
        logistics.setLogisticsNo("SF1234567890");
        logistics.setLogisticsCompany("顺丰速运");
        order.setLogistics(logistics);

        assertNotNull(order.getLogistics());
        assertEquals("SF1234567890", order.getLogistics().getLogisticsNo());
        assertEquals("顺丰速运", order.getLogistics().getLogisticsCompany());
    }

    @Test
    @DisplayName("订单信息 - 产品类型")
    void testOrderInfo_ProductType() {
        OrderInfo order = new OrderInfo();
        order.setProductType(OrderConstants.PRODUCT_TYPE_DIGITAL_PATTERN);

        assertEquals(OrderConstants.PRODUCT_TYPE_DIGITAL_PATTERN, order.getProductType());
    }

    @Test
    @DisplayName("订单信息 - 父订单号")
    void testOrderInfo_ParentOrderNo() {
        OrderInfo order = new OrderInfo();
        order.setOrderNo("ORD202606160001");
        order.setParentOrderNo("ORD202606160000");

        assertEquals("ORD202606160001", order.getOrderNo());
        assertEquals("ORD202606160000", order.getParentOrderNo());
    }
}
