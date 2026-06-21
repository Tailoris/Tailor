package com.tailoris.payment.service.impl;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.*;
import com.alipay.api.response.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tailoris.common.exception.BusinessException;
import com.tailoris.payment.config.AlipayConfig;
import com.tailoris.payment.service.AlipayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 支付宝支付服务实现.
 *
 * <p>支持支付宝多种支付方式：
 * <ul>
 *   <li>电脑网站支付（Page Payment）</li>
 *   <li>扫码支付（QR Code Payment / Precreate）</li>
 *   <li>APP 支付</li>
 *   <li>订单查询</li>
 *   <li>退款</li>
 *   <li>回调验签</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlipayServiceImpl implements AlipayService {

    private final AlipayConfig alipayConfig;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    private volatile AlipayClient alipayClient;

    private static final String PAYMENT_LOCK_KEY = "payment:alipay:lock:";

    private AlipayClient getAlipayClient() {
        if (alipayClient == null) {
            synchronized (this) {
                if (alipayClient == null) {
                    alipayClient = new DefaultAlipayClient(
                            alipayConfig.getGatewayUrl(),
                            alipayConfig.getAppId(),
                            alipayConfig.getPrivateKey(),
                            "json",
                            alipayConfig.getCharset(),
                            alipayConfig.getPublicKey(),
                            alipayConfig.getSignType()
                    );
                }
            }
        }
        return alipayClient;
    }

    private String writeJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new BusinessException("JSON序列化失败: " + e.getMessage());
        }
    }

    // ---- 电脑网站支付 ----

    @Override
    public String createOrder(String orderNo, BigDecimal amount, String subject, String body,
                             String returnUrl, String notifyUrl) {
        String lockKey = PAYMENT_LOCK_KEY + orderNo;
        Boolean lockAcquired = stringRedisTemplate.opsForValue()
                .setIfAbsent(lockKey, "LOCKED", 5, TimeUnit.MINUTES);
        if (Boolean.FALSE.equals(lockAcquired)) {
            throw new BusinessException("订单正在处理中，请稍后");
        }

        try {
            AlipayClient client = getAlipayClient();

            AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();
            request.setReturnUrl(returnUrl != null ? returnUrl : alipayConfig.getReturnUrl());
            request.setNotifyUrl(notifyUrl != null ? notifyUrl : alipayConfig.getNotifyUrl());

            Map<String, Object> bizContent = new LinkedHashMap<>();
            bizContent.put("out_trade_no", orderNo);
            bizContent.put("total_amount", amount.toString());
            bizContent.put("subject", subject);
            bizContent.put("body", body);
            bizContent.put("product_code", "FAST_INSTANT_TRADE_PAY");

            request.setBizContent(writeJson(bizContent));

            AlipayTradePagePayResponse response = client.pageExecute(request);
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

    // ---- 扫码支付 ----

    /**
     * 创建支付宝扫码支付订单.
     */
    public String createQrCodePayment(String orderNo, BigDecimal amount, String subject,
                                       String body, String notifyUrl) {
        String lockKey = PAYMENT_LOCK_KEY + orderNo;
        Boolean lockAcquired = stringRedisTemplate.opsForValue()
                .setIfAbsent(lockKey, "LOCKED", 5, TimeUnit.MINUTES);
        if (Boolean.FALSE.equals(lockAcquired)) {
            throw new BusinessException("订单正在处理中，请稍后");
        }

        try {
            AlipayClient client = getAlipayClient();

            AlipayTradePrecreateRequest request = new AlipayTradePrecreateRequest();
            request.setNotifyUrl(notifyUrl != null ? notifyUrl : alipayConfig.getNotifyUrl());

            Map<String, Object> bizContent = new LinkedHashMap<>();
            bizContent.put("out_trade_no", orderNo);
            bizContent.put("total_amount", amount.toString());
            bizContent.put("subject", subject);
            bizContent.put("body", body);

            request.setBizContent(writeJson(bizContent));

            AlipayTradePrecreateResponse response = client.execute(request);
            if (!response.isSuccess()) {
                throw new BusinessException("支付宝扫码下单失败: " + response.getSubMsg());
            }

            log.info("支付宝扫码支付创建成功: outTradeNo={}, qrCode={}", orderNo, response.getQrCode());
            return response.getQrCode();
        } catch (AlipayApiException e) {
            log.error("支付宝扫码下单异常", e);
            throw new BusinessException("支付宝扫码下单失败: " + e.getMessage());
        } finally {
            stringRedisTemplate.delete(lockKey);
        }
    }

    // ---- APP 支付 ----

    /**
     * 创建支付宝 APP 支付订单.
     */
    public String createAppPayment(String orderNo, BigDecimal amount, String subject,
                                    String body, String notifyUrl) {
        String lockKey = PAYMENT_LOCK_KEY + orderNo;
        Boolean lockAcquired = stringRedisTemplate.opsForValue()
                .setIfAbsent(lockKey, "LOCKED", 5, TimeUnit.MINUTES);
        if (Boolean.FALSE.equals(lockAcquired)) {
            throw new BusinessException("订单正在处理中，请稍后");
        }

        try {
            AlipayClient client = getAlipayClient();

            AlipayTradeAppPayRequest request = new AlipayTradeAppPayRequest();
            request.setNotifyUrl(notifyUrl != null ? notifyUrl : alipayConfig.getNotifyUrl());

            Map<String, Object> bizContent = new LinkedHashMap<>();
            bizContent.put("out_trade_no", orderNo);
            bizContent.put("total_amount", amount.toString());
            bizContent.put("subject", subject);
            bizContent.put("body", body);
            bizContent.put("product_code", "QUICK_MSECURITY_PAY");

            request.setBizContent(writeJson(bizContent));

            AlipayTradeAppPayResponse response = client.sdkExecute(request);
            if (!response.isSuccess()) {
                throw new BusinessException("支付宝APP支付下单失败: " + response.getSubMsg());
            }

            log.info("支付宝APP支付创建成功: outTradeNo={}", orderNo);
            return response.getBody();
        } catch (AlipayApiException e) {
            log.error("支付宝APP支付下单异常", e);
            throw new BusinessException("支付宝APP支付下单失败: " + e.getMessage());
        } finally {
            stringRedisTemplate.delete(lockKey);
        }
    }

    // ---- 查询 ----

    @Override
    public Map<String, Object> queryOrder(String tradeNo, String outTradeNo) {
        try {
            AlipayClient client = getAlipayClient();

            AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
            Map<String, Object> bizContent = new LinkedHashMap<>();

            if (tradeNo != null && !tradeNo.isEmpty()) {
                bizContent.put("trade_no", tradeNo);
            } else if (outTradeNo != null && !outTradeNo.isEmpty()) {
                bizContent.put("out_trade_no", outTradeNo);
            } else {
                throw new BusinessException("必须提供trade_no或out_trade_no");
            }

            request.setBizContent(writeJson(bizContent));

            AlipayTradeQueryResponse response = client.execute(request);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("code", response.getCode());
            result.put("msg", response.getMsg());
            result.put("trade_no", response.getTradeNo());
            result.put("out_trade_no", response.getOutTradeNo());
            result.put("trade_status", response.getTradeStatus());
            result.put("total_amount", response.getTotalAmount());
            result.put("receipt_amount", response.getReceiptAmount());
            result.put("buyer_logon_id", response.getBuyerLogonId());
            result.put("buyer_user_id", response.getBuyerUserId());

            return result;
        } catch (AlipayApiException e) {
            log.error("支付宝查询异常", e);
            throw new BusinessException("支付宝查询失败: " + e.getMessage());
        }
    }

    // ---- 退款 ----

    @Override
    public String refund(String outTradeNo, String outRequestNo, BigDecimal refundAmount, String refundReason) {
        try {
            AlipayClient client = getAlipayClient();

            AlipayTradeRefundRequest request = new AlipayTradeRefundRequest();
            Map<String, Object> bizContent = new LinkedHashMap<>();
            bizContent.put("out_trade_no", outTradeNo);
            bizContent.put("out_request_no", outRequestNo);
            bizContent.put("refund_amount", refundAmount.toString());
            if (refundReason != null && !refundReason.isEmpty()) {
                bizContent.put("refund_reason", refundReason);
            }

            request.setBizContent(writeJson(bizContent));

            AlipayTradeRefundResponse response = client.execute(request);

            if (!response.isSuccess()) {
                throw new BusinessException("支付宝退款失败: " + response.getSubMsg());
            }

            log.info("支付宝退款成功: outTradeNo={}, outRequestNo={}", outTradeNo, outRequestNo);
            return response.getTradeNo();
        } catch (AlipayApiException e) {
            log.error("支付宝退款异常", e);
            throw new BusinessException("支付宝退款失败: " + e.getMessage());
        }
    }

    /**
     * 查询退款.
     */
    public Map<String, Object> queryRefund(String outTradeNo, String outRequestNo) {
        try {
            AlipayClient client = getAlipayClient();

            AlipayTradeFastpayRefundQueryRequest request = new AlipayTradeFastpayRefundQueryRequest();
            Map<String, Object> bizContent = new LinkedHashMap<>();
            bizContent.put("out_trade_no", outTradeNo);
            bizContent.put("out_request_no", outRequestNo);

            request.setBizContent(writeJson(bizContent));

            AlipayTradeFastpayRefundQueryResponse response = client.execute(request);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("trade_no", response.getTradeNo());
            result.put("out_trade_no", response.getOutTradeNo());
            result.put("out_request_no", response.getOutRequestNo());
            result.put("refund_amount", response.getRefundAmount());
            result.put("refund_status", response.getRefundStatus());
            result.put("total_amount", response.getTotalAmount());

            return result;
        } catch (AlipayApiException e) {
            log.error("支付宝退款查询异常", e);
            throw new BusinessException("支付宝退款查询失败: " + e.getMessage());
        }
    }

    // ---- 验签 ----

    @Override
    public boolean verifyCallback(Map<String, String> params) {
        try {
            String signType = params.get("sign_type") != null
                    ? params.get("sign_type")
                    : alipayConfig.getSignType();

            return AlipaySignature.rsaCheckV1(
                    params, alipayConfig.getPublicKey(), alipayConfig.getCharset(), signType);
        } catch (AlipayApiException e) {
            log.error("支付宝回调验签失败", e);
            return false;
        }
    }
}