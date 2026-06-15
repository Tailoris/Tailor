package com.tailoris.payment.service;

import java.math.BigDecimal;
import java.util.Map;

public interface AlipayService {

    String createOrder(String orderNo, BigDecimal amount, String subject, String body, String returnUrl, String notifyUrl);

    Map<String, Object> queryOrder(String tradeNo, String outTradeNo);

    String refund(String outTradeNo, String outRequestNo, BigDecimal refundAmount, String refundReason);

    boolean verifyCallback(Map<String, String> params);
}