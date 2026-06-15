package com.tailoris.payment.service.impl;

import com.tailoris.common.exception.BusinessException;
import com.tailoris.payment.config.WechatPayConfig;
import com.tailoris.payment.service.WechatPayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class WechatPayServiceImpl implements WechatPayService {

    private final WechatPayConfig wechatPayConfig;
    private final RestTemplate restTemplate;
    private final StringRedisTemplate stringRedisTemplate;

    private static final String PAYMENT_LOCK_KEY = "payment:wechat:lock:";

    @Override
    public Map<String, Object> createOrder(String orderNo, BigDecimal amount, String openId, 
                                           String clientIp, String notifyUrl, String body) {
        String lockKey = PAYMENT_LOCK_KEY + orderNo;
        Boolean lockAcquired = stringRedisTemplate.opsForValue().setIfAbsent(lockKey, "LOCKED", 5, java.util.concurrent.TimeUnit.MINUTES);
        if (Boolean.FALSE.equals(lockAcquired)) {
            throw new BusinessException("订单正在处理中，请稍后");
        }

        try {
            Map<String, String> params = new HashMap<>();
            params.put("appid", wechatPayConfig.getAppId());
            params.put("mch_id", wechatPayConfig.getMchId());
            params.put("nonce_str", generateNonceStr());
            params.put("sign_type", wechatPayConfig.getSignType());
            params.put("body", body);
            params.put("out_trade_no", orderNo);
            params.put("total_fee", amount.multiply(new BigDecimal("100")).intValue() + "");
            params.put("spbill_create_ip", clientIp);
            params.put("notify_url", notifyUrl != null ? notifyUrl : wechatPayConfig.getNotifyUrl());
            params.put("trade_type", wechatPayConfig.getTradeType());
            params.put("openid", openId);

            String sign = generateSign(params);
            params.put("sign", sign);

            String xmlData = mapToXml(params);
            String responseXml = restTemplate.postForObject(wechatPayConfig.getUnifiedOrderUrl(), xmlData, String.class);
            
            Map<String, String> responseMap = xmlToMap(responseXml);
            
            if (!"SUCCESS".equals(responseMap.get("return_code"))) {
                throw new BusinessException("微信下单失败: " + responseMap.get("return_msg"));
            }
            if (!"SUCCESS".equals(responseMap.get("result_code"))) {
                throw new BusinessException("微信下单失败: " + responseMap.get("err_code_des"));
            }

            Map<String, Object> result = new HashMap<>();
            result.put("prepay_id", responseMap.get("prepay_id"));
            result.put("nonce_str", params.get("nonce_str"));
            result.put("out_trade_no", orderNo);

            Map<String, String> paySignParams = new HashMap<>();
            paySignParams.put("appId", wechatPayConfig.getAppId());
            paySignParams.put("timeStamp", String.valueOf(System.currentTimeMillis() / 1000));
            paySignParams.put("nonceStr", params.get("nonce_str"));
            paySignParams.put("package", "prepay_id=" + responseMap.get("prepay_id"));
            paySignParams.put("signType", wechatPayConfig.getSignType());
            String paySign = generateSign(paySignParams);

            result.put("paySign", paySign);
            result.put("timeStamp", paySignParams.get("timeStamp"));

            return result;
        } finally {
            stringRedisTemplate.delete(lockKey);
        }
    }

    @Override
    public Map<String, Object> queryOrder(String transactionId, String outTradeNo) {
        Map<String, String> params = new HashMap<>();
        params.put("appid", wechatPayConfig.getAppId());
        params.put("mch_id", wechatPayConfig.getMchId());
        params.put("nonce_str", generateNonceStr());
        
        if (transactionId != null && !transactionId.isEmpty()) {
            params.put("transaction_id", transactionId);
        } else if (outTradeNo != null && !outTradeNo.isEmpty()) {
            params.put("out_trade_no", outTradeNo);
        } else {
            throw new BusinessException("必须提供transaction_id或out_trade_no");
        }

        params.put("sign", generateSign(params));

        String xmlData = mapToXml(params);
        String responseXml = restTemplate.postForObject(wechatPayConfig.getOrderQueryUrl(), xmlData, String.class);
        
        Map<String, String> responseMap = xmlToMap(responseXml);
        
        Map<String, Object> result = new HashMap<>();
        result.putAll(responseMap);
        return result;
    }

    @Override
    public Map<String, Object> refund(String outTradeNo, String outRefundNo, 
                                      BigDecimal refundAmount, BigDecimal totalAmount) {
        Map<String, String> params = new HashMap<>();
        params.put("appid", wechatPayConfig.getAppId());
        params.put("mch_id", wechatPayConfig.getMchId());
        params.put("nonce_str", generateNonceStr());
        params.put("out_trade_no", outTradeNo);
        params.put("out_refund_no", outRefundNo);
        params.put("total_fee", totalAmount.multiply(new BigDecimal("100")).intValue() + "");
        params.put("refund_fee", refundAmount.multiply(new BigDecimal("100")).intValue() + "");

        params.put("sign", generateSign(params));

        String xmlData = mapToXml(params);
        String responseXml = restTemplate.postForObject(wechatPayConfig.getRefundUrl(), xmlData, String.class);
        
        Map<String, String> responseMap = xmlToMap(responseXml);
        
        Map<String, Object> result = new HashMap<>();
        result.putAll(responseMap);
        return result;
    }

    @Override
    public boolean verifyCallback(Map<String, String> params) {
        String sign = params.remove("sign");
        String signType = params.get("sign_type");
        if (signType == null || !signType.equalsIgnoreCase("MD5")) {
            signType = "MD5";
        }
        String generatedSign = generateSign(params);
        return sign != null && sign.equalsIgnoreCase(generatedSign);
    }

    private String generateSign(Map<String, String> params) {
        List<String> keys = new ArrayList<>(params.keySet());
        Collections.sort(keys);
        
        StringBuilder sb = new StringBuilder();
        for (String key : keys) {
            String value = params.get(key);
            if (value != null && !value.isEmpty() && !"sign".equals(key)) {
                sb.append(key).append("=").append(value).append("&");
            }
        }
        sb.append("key=").append(wechatPayConfig.getApiKey());

        return md5(sb.toString()).toUpperCase();
    }

    private String generateNonceStr() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 32);
    }

    private String md5(String data) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] bytes = md.digest(data.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                String hex = Integer.toHexString(b & 0xFF);
                if (hex.length() == 1) {
                    sb.append("0");
                }
                sb.append(hex);
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5算法不存在", e);
        }
    }

    private String mapToXml(Map<String, String> params) {
        StringBuilder sb = new StringBuilder();
        sb.append("<xml>");
        for (Map.Entry<String, String> entry : params.entrySet()) {
            sb.append("<").append(entry.getKey()).append(">");
            sb.append(entry.getValue());
            sb.append("</").append(entry.getKey()).append(">");
        }
        sb.append("</xml>");
        return sb.toString();
    }

    private Map<String, String> xmlToMap(String xml) {
        Map<String, String> map = new HashMap<>();
        String[] pairs = xml.replace("<xml>", "").replace("</xml>", "").split("</[^>]+>");
        
        for (String pair : pairs) {
            if (pair.trim().isEmpty()) continue;
            int start = pair.indexOf(">");
            if (start > 0) {
                String key = pair.substring(1, start);
                String value = pair.substring(start + 1);
                map.put(key, value);
            }
        }
        return map;
    }
}