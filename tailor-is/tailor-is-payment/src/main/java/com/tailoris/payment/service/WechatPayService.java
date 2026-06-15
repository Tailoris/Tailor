package com.tailoris.payment.service;

import java.math.BigDecimal;
import java.util.Map;

public interface WechatPayService {

    Map<String, Object> createOrder(String orderNo, BigDecimal amount, String openId, String clientIp, String notifyUrl, String body);

    Map<String, Object> queryOrder(String transactionId, String outTradeNo);

    Map<String, Object> refund(String outTradeNo, String outRefundNo, BigDecimal refundAmount, BigDecimal totalAmount);

    boolean verifyCallback(Map<String, String> params);
}