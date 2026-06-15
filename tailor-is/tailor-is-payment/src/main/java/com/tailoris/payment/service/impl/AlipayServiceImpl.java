package com.tailoris.payment.service.impl;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.request.AlipayTradeRefundRequest;
import com.alipay.api.response.AlipayTradePagePayResponse;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.alipay.api.response.AlipayTradeRefundResponse;
import com.tailoris.common.exception.BusinessException;
import com.tailoris.payment.config.AlipayConfig;
import com.tailoris.payment.service.AlipayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlipayServiceImpl implements AlipayService {

    private final AlipayConfig alipayConfig;
    private final StringRedisTemplate stringRedisTemplate;

    private static final String PAYMENT_LOCK_KEY = "payment:alipay:lock:";

    @Override
    public String createOrder(String orderNo, BigDecimal amount, String subject, String body, 
                             String returnUrl, String notifyUrl) {
        String lockKey = PAYMENT_LOCK_KEY + orderNo;
        Boolean lockAcquired = stringRedisTemplate.opsForValue().setIfAbsent(lockKey, "LOCKED", 5, java.util.concurrent.TimeUnit.MINUTES);
        if (Boolean.FALSE.equals(lockAcquired)) {
            throw new BusinessException("订单正在处理中，请稍后");
        }

        try {
            AlipayClient alipayClient = new DefaultAlipayClient(
                    alipayConfig.getGatewayUrl(),
                    alipayConfig.getAppId(),
                    alipayConfig.getPrivateKey(),
                    "json",
                    alipayConfig.getCharset(),
                    alipayConfig.getPublicKey(),
                    alipayConfig.getSignType()
            );

            AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();
            request.setReturnUrl(returnUrl != null ? returnUrl : alipayConfig.getReturnUrl());
            request.setNotifyUrl(notifyUrl != null ? notifyUrl : alipayConfig.getNotifyUrl());

            Map<String, Object> bizContent = new HashMap<>();
            bizContent.put("out_trade_no", orderNo);
            bizContent.put("total_amount", amount.toString());
            bizContent.put("subject", subject);
            bizContent.put("body", body);
            bizContent.put("product_code", "FAST_INSTANT_TRADE_PAY");

            request.setBizContent(com.alibaba.fastjson.JSONObject.toJSONString(bizContent));

            AlipayTradePagePayResponse response = alipayClient.pageExecute(request);
            if (!response.isSuccess()) {
                throw new BusinessException("支付宝下单失败: " + response.getSubMsg());
            }

            return response.getBody();
        } catch (AlipayApiException e) {
            log.error("支付宝下单异常", e);
            throw new BusinessException("支付宝下单失败: " + e.getMessage());
        } finally {
            stringRedisTemplate.delete(lockKey);
        }
    }

    @Override
    public Map<String, Object> queryOrder(String tradeNo, String outTradeNo) {
        try {
            AlipayClient alipayClient = new DefaultAlipayClient(
                    alipayConfig.getGatewayUrl(),
                    alipayConfig.getAppId(),
                    alipayConfig.getPrivateKey(),
                    "json",
                    alipayConfig.getCharset(),
                    alipayConfig.getPublicKey(),
                    alipayConfig.getSignType()
            );

            AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
            Map<String, Object> bizContent = new HashMap<>();
            
            if (tradeNo != null && !tradeNo.isEmpty()) {
                bizContent.put("trade_no", tradeNo);
            } else if (outTradeNo != null && !outTradeNo.isEmpty()) {
                bizContent.put("out_trade_no", outTradeNo);
            } else {
                throw new BusinessException("必须提供trade_no或out_trade_no");
            }

            request.setBizContent(com.alibaba.fastjson.JSONObject.toJSONString(bizContent));

            AlipayTradeQueryResponse response = alipayClient.execute(request);
            
            Map<String, Object> result = new HashMap<>();
            result.put("code", response.getCode());
            result.put("msg", response.getMsg());
            result.put("trade_no", response.getTradeNo());
            result.put("out_trade_no", response.getOutTradeNo());
            result.put("trade_status", response.getTradeStatus());
            result.put("total_amount", response.getTotalAmount());
            result.put("receipt_amount", response.getReceiptAmount());
            
            return result;
        } catch (AlipayApiException e) {
            log.error("支付宝查询异常", e);
            throw new BusinessException("支付宝查询失败: " + e.getMessage());
        }
    }

    @Override
    public String refund(String outTradeNo, String outRequestNo, BigDecimal refundAmount, String refundReason) {
        try {
            AlipayClient alipayClient = new DefaultAlipayClient(
                    alipayConfig.getGatewayUrl(),
                    alipayConfig.getAppId(),
                    alipayConfig.getPrivateKey(),
                    "json",
                    alipayConfig.getCharset(),
                    alipayConfig.getPublicKey(),
                    alipayConfig.getSignType()
            );

            AlipayTradeRefundRequest request = new AlipayTradeRefundRequest();
            Map<String, Object> bizContent = new HashMap<>();
            bizContent.put("out_trade_no", outTradeNo);
            bizContent.put("out_request_no", outRequestNo);
            bizContent.put("refund_amount", refundAmount.toString());
            if (refundReason != null && !refundReason.isEmpty()) {
                bizContent.put("refund_reason", refundReason);
            }

            request.setBizContent(com.alibaba.fastjson.JSONObject.toJSONString(bizContent));

            AlipayTradeRefundResponse response = alipayClient.execute(request);
            
            if (!response.isSuccess()) {
                throw new BusinessException("支付宝退款失败: " + response.getSubMsg());
            }

            return response.getTradeNo();
        } catch (AlipayApiException e) {
            log.error("支付宝退款异常", e);
            throw new BusinessException("支付宝退款失败: " + e.getMessage());
        }
    }

    @Override
    public boolean verifyCallback(Map<String, String> params) {
        try {
            String signType = params.get("sign_type") != null ? params.get("sign_type") : alipayConfig.getSignType();
            
            return AlipaySignature.rsaCheckV1(params, alipayConfig.getPublicKey(), alipayConfig.getCharset(), signType);
        } catch (AlipayApiException e) {
            log.error("支付宝回调验签失败", e);
            return false;
        }
    }
}