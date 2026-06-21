package com.tailoris.payment.service.impl;

import com.tailoris.common.exception.BusinessException;
import com.tailoris.payment.config.WechatPayConfig;
import com.tailoris.payment.service.WechatPayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 微信支付 V3 API 实现.
 *
 * <p>使用微信支付 API V3 版本，支持：
 * <ul>
 *   <li>Native 支付（扫码支付）</li>
 *   <li>JSAPI 支付（公众号/小程序内支付）</li>
 *   <li>签名生成与验证（RSA with SHA256）</li>
 *   <li>回调通知处理与 AES-256-GCM 解密</li>
 * </ul>
 *
 * <p>V3 相比 V2 的改进：
 * <ul>
 *   <li>JSON 格式替代 XML 格式</li>
 *   <li>RSA 签名替代 MD5 签名，安全性更高</li>
 *   <li>AES-256-GCM 加密回调敏感数据</li>
 *   <li>平台证书管理</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WechatPayServiceImpl implements WechatPayService {

    private final WechatPayConfig wechatPayConfig;
    private final RestTemplate restTemplate;
    private final StringRedisTemplate stringRedisTemplate;

    private static final String PAYMENT_LOCK_KEY = "payment:wechat:lock:";
    private static final String NATIVE_PAY_URL = "https://api.mch.weixin.qq.com/v3/pay/transactions/native";
    private static final String JSAPI_PAY_URL = "https://api.mch.weixin.qq.com/v3/pay/transactions/jsapi";
    private static final String ORDER_QUERY_URL = "https://api.mch.weixin.qq.com/v3/pay/transactions/out-trade-no/%s?mchid=%s";
    private static final String TRANSACTION_QUERY_URL = "https://api.mch.weixin.qq.com/v3/pay/transactions/id/%s?mchid=%s";
    private static final String CLOSE_ORDER_URL = "https://api.mch.weixin.qq.com/v3/pay/transactions/out-trade-no/%s/close";
    private static final String REFUND_URL = "https://api.mch.weixin.qq.com/v3/refund/domestic/refunds";
    private static final String REFUND_QUERY_URL = "https://api.mch.weixin.qq.com/v3/refund/domestic/refunds/%s";

    // ---- V3 API 新方法 ----

    @Override
    public Map<String, Object> createPayment(String orderNo, BigDecimal amount, String body,
                                              String clientIp, String notifyUrl) {
        String lockKey = PAYMENT_LOCK_KEY + orderNo;
        Boolean lockAcquired = stringRedisTemplate.opsForValue()
                .setIfAbsent(lockKey, "LOCKED", 5, TimeUnit.MINUTES);
        if (Boolean.FALSE.equals(lockAcquired)) {
            throw new BusinessException("订单正在处理中，请稍后");
        }

        try {
            Map<String, Object> requestBody = new LinkedHashMap<>();
            requestBody.put("appid", wechatPayConfig.getAppId());
            requestBody.put("mchid", wechatPayConfig.getMchId());
            requestBody.put("description", body != null ? body : "Tailor IS 订单支付");
            requestBody.put("out_trade_no", orderNo);
            requestBody.put("notify_url", notifyUrl != null ? notifyUrl : wechatPayConfig.getNotifyUrl());

            Map<String, Object> amountObj = new LinkedHashMap<>();
            amountObj.put("total", amount.multiply(new BigDecimal("100")).intValue());
            amountObj.put("currency", "CNY");
            requestBody.put("amount", amountObj);

            String jsonBody = mapToJson(requestBody);
            String token = generateAuthorizationHeader("POST", NATIVE_PAY_URL, jsonBody);

            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
            headers.set("Authorization", token);
            headers.set("Accept", "application/json");

            org.springframework.http.HttpEntity<String> entity =
                    new org.springframework.http.HttpEntity<>(jsonBody, headers);
            String response = restTemplate.postForObject(NATIVE_PAY_URL, entity, String.class);

            Map<String, Object> result = parseJsonResponse(response);
            result.put("paymentNo", orderNo);
            return result;
        } catch (Exception e) {
            log.error("微信Native支付创建失败: orderNo={}", orderNo, e);
            throw new BusinessException("微信支付创建失败: " + e.getMessage());
        } finally {
            stringRedisTemplate.delete(lockKey);
        }
    }

    @Override
    public Map<String, Object> createJsapiPayment(String orderNo, BigDecimal amount, String openId,
                                                   String body, String clientIp, String notifyUrl) {
        String lockKey = PAYMENT_LOCK_KEY + orderNo;
        Boolean lockAcquired = stringRedisTemplate.opsForValue()
                .setIfAbsent(lockKey, "LOCKED", 5, TimeUnit.MINUTES);
        if (Boolean.FALSE.equals(lockAcquired)) {
            throw new BusinessException("订单正在处理中，请稍后");
        }

        try {
            Map<String, Object> requestBody = new LinkedHashMap<>();
            requestBody.put("appid", wechatPayConfig.getAppId());
            requestBody.put("mchid", wechatPayConfig.getMchId());
            requestBody.put("description", body != null ? body : "Tailor IS 订单支付");
            requestBody.put("out_trade_no", orderNo);
            requestBody.put("notify_url", notifyUrl != null ? notifyUrl : wechatPayConfig.getNotifyUrl());

            Map<String, Object> amountObj = new LinkedHashMap<>();
            amountObj.put("total", amount.multiply(new BigDecimal("100")).intValue());
            amountObj.put("currency", "CNY");
            requestBody.put("amount", amountObj);

            Map<String, Object> payerObj = new LinkedHashMap<>();
            payerObj.put("openid", openId);
            requestBody.put("payer", payerObj);

            String jsonBody = mapToJson(requestBody);
            String token = generateAuthorizationHeader("POST", JSAPI_PAY_URL, jsonBody);

            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
            headers.set("Authorization", token);
            headers.set("Accept", "application/json");

            org.springframework.http.HttpEntity<String> entity =
                    new org.springframework.http.HttpEntity<>(jsonBody, headers);
            String response = restTemplate.postForObject(JSAPI_PAY_URL, entity, String.class);

            Map<String, Object> result = parseJsonResponse(response);

            // 生成 JSAPI 调起支付参数
            String prepayId = (String) result.get("prepay_id");
            String timeStamp = String.valueOf(Instant.now().getEpochSecond());
            String nonceStr = generateNonceStr();
            String packageStr = "prepay_id=" + prepayId;

            String signStr = wechatPayConfig.getAppId() + "\n"
                    + timeStamp + "\n"
                    + nonceStr + "\n"
                    + packageStr + "\n";
            String paySign = signWithRsa(signStr);

            result.put("paySign", paySign);
            result.put("timeStamp", timeStamp);
            result.put("nonceStr", nonceStr);
            result.put("package", packageStr);
            result.put("paymentNo", orderNo);

            return result;
        } catch (Exception e) {
            log.error("微信JSAPI支付创建失败: orderNo={}", orderNo, e);
            throw new BusinessException("微信支付创建失败: " + e.getMessage());
        } finally {
            stringRedisTemplate.delete(lockKey);
        }
    }

    @Override
    public Map<String, Object> queryPayment(String transactionId, String outTradeNo) {
        try {
            String url;
            if (transactionId != null && !transactionId.isEmpty()) {
                url = String.format(TRANSACTION_QUERY_URL, transactionId, wechatPayConfig.getMchId());
            } else if (outTradeNo != null && !outTradeNo.isEmpty()) {
                url = String.format(ORDER_QUERY_URL, outTradeNo, wechatPayConfig.getMchId());
            } else {
                throw new BusinessException("必须提供 transactionId 或 outTradeNo");
            }

            String token = generateAuthorizationHeader("GET", url, "");

            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.set("Authorization", token);
            headers.set("Accept", "application/json");

            org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>(headers);
            String response = restTemplate.exchange(url,
                    org.springframework.http.HttpMethod.GET, entity, String.class).getBody();

            return parseJsonResponse(response);
        } catch (Exception e) {
            log.error("微信支付查询失败: transactionId={}, outTradeNo={}", transactionId, outTradeNo, e);
            throw new BusinessException("微信支付查询失败: " + e.getMessage());
        }
    }

    @Override
    public boolean closePayment(String outTradeNo) {
        try {
            String url = String.format(CLOSE_ORDER_URL, outTradeNo);

            Map<String, Object> requestBody = new LinkedHashMap<>();
            requestBody.put("mchid", wechatPayConfig.getMchId());
            String jsonBody = mapToJson(requestBody);

            String token = generateAuthorizationHeader("POST", url, jsonBody);

            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
            headers.set("Authorization", token);
            headers.set("Accept", "application/json");

            org.springframework.http.HttpEntity<String> entity =
                    new org.springframework.http.HttpEntity<>(jsonBody, headers);
            restTemplate.postForObject(url, entity, String.class);

            log.info("微信支付订单关闭成功: outTradeNo={}", outTradeNo);
            return true;
        } catch (Exception e) {
            log.warn("微信支付订单关闭失败: outTradeNo={}", outTradeNo, e);
            return false;
        }
    }

    @Override
    public Map<String, Object> createRefund(String outTradeNo, String outRefundNo,
                                             BigDecimal refundAmount, BigDecimal totalAmount, String reason) {
        try {
            Map<String, Object> requestBody = new LinkedHashMap<>();
            requestBody.put("out_trade_no", outTradeNo);
            requestBody.put("out_refund_no", outRefundNo);
            requestBody.put("reason", reason != null ? reason : "用户退款");

            Map<String, Object> amountObj = new LinkedHashMap<>();
            amountObj.put("refund", refundAmount.multiply(new BigDecimal("100")).intValue());
            amountObj.put("total", totalAmount.multiply(new BigDecimal("100")).intValue());
            amountObj.put("currency", "CNY");
            requestBody.put("amount", amountObj);

            String jsonBody = mapToJson(requestBody);
            String token = generateAuthorizationHeader("POST", REFUND_URL, jsonBody);

            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
            headers.set("Authorization", token);
            headers.set("Accept", "application/json");

            org.springframework.http.HttpEntity<String> entity =
                    new org.springframework.http.HttpEntity<>(jsonBody, headers);
            String response = restTemplate.postForObject(REFUND_URL, entity, String.class);

            Map<String, Object> result = parseJsonResponse(response);
            log.info("微信退款创建成功: outTradeNo={}, outRefundNo={}", outTradeNo, outRefundNo);
            return result;
        } catch (Exception e) {
            log.error("微信退款创建失败: outTradeNo={}", outTradeNo, e);
            throw new BusinessException("微信退款失败: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> queryRefund(String outRefundNo) {
        try {
            String url = String.format(REFUND_QUERY_URL, outRefundNo);
            String token = generateAuthorizationHeader("GET", url, "");

            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.set("Authorization", token);
            headers.set("Accept", "application/json");

            org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>(headers);
            String response = restTemplate.exchange(url,
                    org.springframework.http.HttpMethod.GET, entity, String.class).getBody();

            return parseJsonResponse(response);
        } catch (Exception e) {
            log.error("微信退款查询失败: outRefundNo={}", outRefundNo, e);
            throw new BusinessException("微信退款查询失败: " + e.getMessage());
        }
    }

    @Override
    public boolean verifyCallback(String body, String signature, String timestamp,
                                   String nonce, String serialNo) {
        try {
            // 构建签名串
            String signStr = timestamp + "\n" + nonce + "\n" + body + "\n";
            return verifyRsaSign(signStr, signature, wechatPayConfig.getApiV3Key());
        } catch (Exception e) {
            log.error("微信回调验签异常", e);
            return false;
        }
    }

    @Override
    public String decryptCallbackData(String ciphertext, String nonce, String associatedData) {
        try {
            byte[] cipherBytes = Base64.getDecoder().decode(ciphertext);
            byte[] keyBytes = wechatPayConfig.getApiV3Key().getBytes(StandardCharsets.UTF_8);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
            GCMParameterSpec gcmSpec = new GCMParameterSpec(128, nonce.getBytes(StandardCharsets.UTF_8));

            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec);
            if (associatedData != null && !associatedData.isEmpty()) {
                cipher.updateAAD(associatedData.getBytes(StandardCharsets.UTF_8));
            }

            byte[] plainBytes = cipher.doFinal(cipherBytes);
            return new String(plainBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("微信回调数据解密失败", e);
            throw new BusinessException("回调数据解密失败");
        }
    }

    // ---- 签名相关 ----

    /**
     * 生成 V3 API Authorization 请求头.
     */
    private String generateAuthorizationHeader(String method, String url, String body) {
        String nonceStr = generateNonceStr();
        long timestamp = Instant.now().getEpochSecond();

        String signStr = method + "\n"
                + url.substring(url.indexOf("/v3/")) + "\n"
                + timestamp + "\n"
                + nonceStr + "\n"
                + body + "\n";

        String signature = signWithRsa(signStr);

        return "WECHATPAY2-SHA256-RSA2048 "
                + "mchid=\"" + wechatPayConfig.getMchId() + "\","
                + "nonce_str=\"" + nonceStr + "\","
                + "signature=\"" + signature + "\","
                + "timestamp=\"" + timestamp + "\","
                + "serial_no=\"" + wechatPayConfig.getCertificateSerialNo() + "\"";
    }

    /**
     * RSA with SHA256 签名.
     */
    private String signWithRsa(String data) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(wechatPayConfig.getPrivateKey());
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PrivateKey privateKey = keyFactory.generatePrivate(keySpec);

            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(privateKey);
            signature.update(data.getBytes(StandardCharsets.UTF_8));

            return Base64.getEncoder().encodeToString(signature.sign());
        } catch (Exception e) {
            throw new RuntimeException("RSA签名失败", e);
        }
    }

    /**
     * RSA with SHA256 验签.
     */
    private boolean verifyRsaSign(String data, String sign, String publicKeyStr) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(publicKeyStr);
            java.security.spec.X509EncodedKeySpec keySpec =
                    new java.security.spec.X509EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PublicKey publicKey = keyFactory.generatePublic(keySpec);

            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initVerify(publicKey);
            signature.update(data.getBytes(StandardCharsets.UTF_8));

            return signature.verify(Base64.getDecoder().decode(sign));
        } catch (Exception e) {
            return false;
        }
    }

    private String generateNonceStr() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 32);
    }

    // ---- JSON 工具方法 ----

    @SuppressWarnings("unchecked")
    private String mapToJson(Map<String, Object> map) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!first) sb.append(",");
            first = false;
            sb.append("\"").append(entry.getKey()).append("\":");
            Object value = entry.getValue();
            if (value instanceof String) {
                sb.append("\"").append(escapeJson((String) value)).append("\"");
            } else if (value instanceof Number) {
                sb.append(value);
            } else if (value instanceof Boolean) {
                sb.append(value);
            } else if (value instanceof Map) {
                sb.append(mapToJson((Map<String, Object>) value));
            } else {
                sb.append("\"").append(escapeJson(String.valueOf(value))).append("\"");
            }
        }
        sb.append("}");
        return sb.toString();
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseJsonResponse(String json) {
        // 简化 JSON 解析；生产环境建议使用 Jackson
        Map<String, Object> result = new LinkedHashMap<>();
        json = json.trim();
        if (json.startsWith("{")) {
            json = json.substring(1);
        }
        if (json.endsWith("}")) {
            json = json.substring(0, json.length() - 1);
        }
        // 简单键值对解析
        String[] pairs = json.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
        for (String pair : pairs) {
            pair = pair.trim();
            int colonIdx = pair.indexOf(':');
            if (colonIdx < 0) continue;
            String key = pair.substring(0, colonIdx).trim().replaceAll("^\"|\"$", "");
            String value = pair.substring(colonIdx + 1).trim();

            if (value.startsWith("\"")) {
                value = value.substring(1, value.length() - 1);
                result.put(key, value);
            } else if (value.equals("true") || value.equals("false")) {
                result.put(key, Boolean.parseBoolean(value));
            } else if (value.startsWith("{")) {
                result.put(key, parseJsonResponse(value));
            } else {
                try {
                    result.put(key, Long.parseLong(value));
                } catch (NumberFormatException e) {
                    result.put(key, value);
                }
            }
        }
        return result;
    }
}