package com.tailoris.payment.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("支付记录实体测试")
class PaymentRecordTest {

    @Test
    @DisplayName("创建支付记录 - 基本属性")
    void testPaymentRecord_BasicProperties() {
        PaymentRecord record = new PaymentRecord();
        record.setId(1L);
        record.setOrderId(100L);
        record.setOrderNo("ORD202606160001");
        record.setPaymentNo("PAY202606160001");
        record.setUserId(50L);
        record.setAmount(new BigDecimal("99.99"));

        assertEquals(1L, record.getId());
        assertEquals(100L, record.getOrderId());
        assertEquals("ORD202606160001", record.getOrderNo());
        assertEquals("PAY202606160001", record.getPaymentNo());
        assertEquals(50L, record.getUserId());
        assertEquals(new BigDecimal("99.99"), record.getAmount());
    }

    @Test
    @DisplayName("创建支付记录 - 支付渠道")
    void testPaymentRecord_PayChannel() {
        PaymentRecord record = new PaymentRecord();
        record.setPayChannel(1); // 微信
        record.setPayMethod("JSAPI");

        assertEquals(1, record.getPayChannel());
        assertEquals("JSAPI", record.getPayMethod());
    }

    @Test
    @DisplayName("创建支付记录 - 支付状态")
    void testPaymentRecord_PayStatus() {
        PaymentRecord record = new PaymentRecord();
        record.setPayStatus(0); // 待支付

        assertEquals(0, record.getPayStatus());
    }

    @Test
    @DisplayName("创建支付记录 - 支付时间")
    void testPaymentRecord_PayTime() {
        PaymentRecord record = new PaymentRecord();
        LocalDateTime now = LocalDateTime.now();
        record.setPayTime(now);
        record.setExpireTime(now.plusMinutes(30));

        assertEquals(now, record.getPayTime());
        assertEquals(now.plusMinutes(30), record.getExpireTime());
    }

    @Test
    @DisplayName("创建支付记录 - 交易ID")
    void testPaymentRecord_TransactionId() {
        PaymentRecord record = new PaymentRecord();
        record.setTransactionId("4200001234567890abcdef");

        assertEquals("4200001234567890abcdef", record.getTransactionId());
    }

    @Test
    @DisplayName("创建支付记录 - 渠道请求响应")
    void testPaymentRecord_ChannelRequestResponse() {
        PaymentRecord record = new PaymentRecord();
        record.setChannelRequest("{\"appid\":\"wx123\",\"body\":\"商品购买\"}");
        record.setChannelResponse("{\"return_code\":\"SUCCESS\",\"prepay_id\":\"wx123\"}");

        assertNotNull(record.getChannelRequest());
        assertNotNull(record.getChannelResponse());
        assertTrue(record.getChannelRequest().contains("wx123"));
        assertTrue(record.getChannelResponse().contains("SUCCESS"));
    }

    @Test
    @DisplayName("创建支付记录 - 回调信息")
    void testPaymentRecord_NotifyInfo() {
        PaymentRecord record = new PaymentRecord();
        record.setNotifyUrl("https://api.tailoris.com/payment/notify");
        record.setNotifyStatus(1);
        record.setNotifyTime(LocalDateTime.now());

        assertEquals("https://api.tailoris.com/payment/notify", record.getNotifyUrl());
        assertEquals(1, record.getNotifyStatus());
        assertNotNull(record.getNotifyTime());
    }

    @Test
    @DisplayName("创建支付记录 - 客户端信息")
    void testPaymentRecord_ClientInfo() {
        PaymentRecord record = new PaymentRecord();
        record.setClientIp("192.168.1.100");
        record.setDeviceType(1); // PC

        assertEquals("192.168.1.100", record.getClientIp());
        assertEquals(1, record.getDeviceType());
    }

    @Test
    @DisplayName("创建支付记录 - 备注")
    void testPaymentRecord_Remark() {
        PaymentRecord record = new PaymentRecord();
        record.setRemark("订单支付");

        assertEquals("订单支付", record.getRemark());
    }

    @Test
    @DisplayName("创建支付记录 - 大金额")
    void testPaymentRecord_LargeAmount() {
        PaymentRecord record = new PaymentRecord();
        record.setAmount(new BigDecimal("999999.99"));

        assertEquals(new BigDecimal("999999.99"), record.getAmount());
    }

    @Test
    @DisplayName("创建支付记录 - 小金额")
    void testPaymentRecord_SmallAmount() {
        PaymentRecord record = new PaymentRecord();
        record.setAmount(new BigDecimal("0.01"));

        assertEquals(new BigDecimal("0.01"), record.getAmount());
    }

    @Test
    @DisplayName("创建支付记录 - 零金额")
    void testPaymentRecord_ZeroAmount() {
        PaymentRecord record = new PaymentRecord();
        record.setAmount(BigDecimal.ZERO);

        assertEquals(BigDecimal.ZERO, record.getAmount());
    }

    @Test
    @DisplayName("创建支付记录 - 不同支付渠道")
    void testPaymentRecord_DifferentPayChannels() {
        PaymentRecord record = new PaymentRecord();

        // 微信支付
        record.setPayChannel(1);
        assertEquals(1, record.getPayChannel());

        // 支付宝
        record.setPayChannel(2);
        assertEquals(2, record.getPayChannel());

        // 银行卡
        record.setPayChannel(3);
        assertEquals(3, record.getPayChannel());

        // 余额
        record.setPayChannel(4);
        assertEquals(4, record.getPayChannel());
    }

    @Test
    @DisplayName("创建支付记录 - 不同设备类型")
    void testPaymentRecord_DifferentDeviceTypes() {
        PaymentRecord record = new PaymentRecord();

        // PC
        record.setDeviceType(1);
        assertEquals(1, record.getDeviceType());

        // H5
        record.setDeviceType(2);
        assertEquals(2, record.getDeviceType());

        // 小程序
        record.setDeviceType(3);
        assertEquals(3, record.getDeviceType());

        // APP
        record.setDeviceType(4);
        assertEquals(4, record.getDeviceType());
    }
}
