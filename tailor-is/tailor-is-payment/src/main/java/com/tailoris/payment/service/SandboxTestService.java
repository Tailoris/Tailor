package com.tailoris.payment.service;

import java.math.BigDecimal;
import java.util.Map;

public interface SandboxTestService {

    Map<String, Object> testWechatPay(String orderNo, BigDecimal amount, String openId);

    Map<String, Object> testAlipay(String orderNo, BigDecimal amount, String subject);

    Map<String, Object> testWechatRefund(String outTradeNo, BigDecimal refundAmount);

    Map<String, Object> testAlipayRefund(String outTradeNo, BigDecimal refundAmount);

    Map<String, Object> verifyPaymentStatus(String paymentNo);
}