package com.tailoris.payment.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("支付请求DTO测试")
class PayRequestTest {

    @Test
    @DisplayName("支付请求 - 基本属性")
    void testPayRequest_BasicProperties() {
        PayRequest request = new PayRequest();
        request.setOrderId(100L);
        request.setAmount(new BigDecimal("99.99"));
        request.setPayChannel(1);
        request.setPayMethod("JSAPI");
        request.setDeviceType(1);
        request.setNotifyUrl("https://api.tailoris.com/payment/notify");
        request.setRemark("订单支付");
        request.setOpenId("oUpF8uMuAJO_M2pxb1Q9zNjWeS6o");
        request.setBody("商品购买");
        request.setSubject("定制服装");

        assertEquals(100L, request.getOrderId());
        assertEquals(new BigDecimal("99.99"), request.getAmount());
        assertEquals(1, request.getPayChannel());
        assertEquals("JSAPI", request.getPayMethod());
        assertEquals(1, request.getDeviceType());
        assertEquals("https://api.tailoris.com/payment/notify", request.getNotifyUrl());
        assertEquals("订单支付", request.getRemark());
        assertEquals("oUpF8uMuAJO_M2pxb1Q9zNjWeS6o", request.getOpenId());
        assertEquals("商品购买", request.getBody());
        assertEquals("定制服装", request.getSubject());
    }

    @Test
    @DisplayName("支付请求 - 订单ID")
    void testPayRequest_OrderId() {
        PayRequest request = new PayRequest();
        request.setOrderId(100L);

        assertEquals(100L, request.getOrderId());
    }

    @Test
    @DisplayName("支付请求 - 支付金额")
    void testPayRequest_Amount() {
        PayRequest request = new PayRequest();
        request.setAmount(new BigDecimal("100.00"));

        assertEquals(new BigDecimal("100.00"), request.getAmount());
    }

    @Test
    @DisplayName("支付请求 - 最小金额")
    void testPayRequest_MinAmount() {
        PayRequest request = new PayRequest();
        request.setAmount(new BigDecimal("0.01"));

        assertEquals(new BigDecimal("0.01"), request.getAmount());
    }

    @Test
    @DisplayName("支付请求 - 大金额")
    void testPayRequest_LargeAmount() {
        PayRequest request = new PayRequest();
        request.setAmount(new BigDecimal("999999.99"));

        assertEquals(new BigDecimal("999999.99"), request.getAmount());
    }

    @Test
    @DisplayName("支付请求 - 支付渠道")
    void testPayRequest_PayChannel() {
        PayRequest request = new PayRequest();

        // 微信
        request.setPayChannel(1);
        assertEquals(1, request.getPayChannel());

        // 支付宝
        request.setPayChannel(2);
        assertEquals(2, request.getPayChannel());

        // 银行卡
        request.setPayChannel(3);
        assertEquals(3, request.getPayChannel());

        // 余额
        request.setPayChannel(4);
        assertEquals(4, request.getPayChannel());

        // Apple Pay
        request.setPayChannel(5);
        assertEquals(5, request.getPayChannel());

        // 银联
        request.setPayChannel(6);
        assertEquals(6, request.getPayChannel());
    }

    @Test
    @DisplayName("支付请求 - 支付方式")
    void testPayRequest_PayMethod() {
        PayRequest request = new PayRequest();
        request.setPayMethod("JSAPI");

        assertEquals("JSAPI", request.getPayMethod());
    }

    @Test
    @DisplayName("支付请求 - 设备类型")
    void testPayRequest_DeviceType() {
        PayRequest request = new PayRequest();

        // PC
        request.setDeviceType(1);
        assertEquals(1, request.getDeviceType());

        // H5
        request.setDeviceType(2);
        assertEquals(2, request.getDeviceType());

        // 小程序
        request.setDeviceType(3);
        assertEquals(3, request.getDeviceType());

        // APP
        request.setDeviceType(4);
        assertEquals(4, request.getDeviceType());
    }

    @Test
    @DisplayName("支付请求 - 回调地址")
    void testPayRequest_NotifyUrl() {
        PayRequest request = new PayRequest();
        request.setNotifyUrl("https://api.tailoris.com/payment/notify/wechat");

        assertEquals("https://api.tailoris.com/payment/notify/wechat", request.getNotifyUrl());
    }

    @Test
    @DisplayName("支付请求 - 备注")
    void testPayRequest_Remark() {
        PayRequest request = new PayRequest();
        request.setRemark("VIP用户订单支付");

        assertEquals("VIP用户订单支付", request.getRemark());
    }

    @Test
    @DisplayName("支付请求 - 微信OpenID")
    void testPayRequest_OpenId() {
        PayRequest request = new PayRequest();
        request.setOpenId("oUpF8uMuAJO_M2pxb1Q9zNjWeS6o");

        assertEquals("oUpF8uMuAJO_M2pxb1Q9zNjWeS6o", request.getOpenId());
    }

    @Test
    @DisplayName("支付请求 - 商品描述")
    void testPayRequest_Body() {
        PayRequest request = new PayRequest();
        request.setBody("定制西装一套");

        assertEquals("定制西装一套", request.getBody());
    }

    @Test
    @DisplayName("支付请求 - 商品标题")
    void testPayRequest_Subject() {
        PayRequest request = new PayRequest();
        request.setSubject("Tailor IS 定制服装");

        assertEquals("Tailor IS 定制服装", request.getSubject());
    }

    @Test
    @DisplayName("支付请求 - 可选属性为空")
    void testPayRequest_OptionalFieldsNull() {
        PayRequest request = new PayRequest();
        request.setOrderId(100L);
        request.setAmount(new BigDecimal("99.99"));
        request.setPayChannel(1);

        assertNotNull(request.getOrderId());
        assertNotNull(request.getAmount());
        assertNotNull(request.getPayChannel());
        assertNull(request.getPayMethod());
        assertNull(request.getDeviceType());
        assertNull(request.getNotifyUrl());
        assertNull(request.getRemark());
        assertNull(request.getOpenId());
        assertNull(request.getBody());
        assertNull(request.getSubject());
    }

    @Test
    @DisplayName("支付请求 - 仅必填字段")
    void testPayRequest_RequiredFieldsOnly() {
        PayRequest request = new PayRequest();
        request.setOrderId(100L);
        request.setAmount(new BigDecimal("99.99"));
        request.setPayChannel(1);

        assertNotNull(request.getOrderId());
        assertNotNull(request.getAmount());
        assertNotNull(request.getPayChannel());
    }
}
