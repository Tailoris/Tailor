package com.tailoris.payment.service.impl;

import com.tailoris.common.exception.BusinessException;
import com.tailoris.payment.config.WechatPayConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("WechatPayServiceImpl 单元测试")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WechatPayServiceImplTest {

    @Mock
    private WechatPayConfig wechatPayConfig;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private WechatPayServiceImpl wechatPayService;

    @BeforeEach
    void setUp() {
        lenient().when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(wechatPayConfig.getAppId()).thenReturn("wx_test_appid");
        lenient().when(wechatPayConfig.getMchId()).thenReturn("1234567890");
        lenient().when(wechatPayConfig.getApiKey()).thenReturn("test_api_key_1234567890123456");
        lenient().when(wechatPayConfig.getSignType()).thenReturn("MD5");
        lenient().when(wechatPayConfig.getTradeType()).thenReturn("JSAPI");
        lenient().when(wechatPayConfig.getUnifiedOrderUrl()).thenReturn("https://api.mch.weixin.qq.com/pay/unifiedorder");
        lenient().when(wechatPayConfig.getOrderQueryUrl()).thenReturn("https://api.mch.weixin.qq.com/pay/orderquery");
        lenient().when(wechatPayConfig.getRefundUrl()).thenReturn("https://api.mch.weixin.qq.com/secapi/pay/refund");
        lenient().when(wechatPayConfig.getNotifyUrl()).thenReturn("https://api.tailoris.com/api/v1/payment/wechat/callback");
    }

    @Test
    @DisplayName("创建微信支付订单-成功")
    void testCreateOrder_Success() {
        when(valueOperations.setIfAbsent(anyString(), eq("LOCKED"), eq(5L), eq(TimeUnit.MINUTES))).thenReturn(true);
        when(stringRedisTemplate.delete(anyString())).thenReturn(true);

        // 使用简单 XML，不使用 CDATA（因为源码 xmlToMap 不支持 CDATA）
        String successXml = "<xml>" +
                "<return_code>SUCCESS</return_code>" +
                "<result_code>SUCCESS</result_code>" +
                "<prepay_id>wx201410272009395522657a690389285100</prepay_id>" +
                "</xml>";
        when(restTemplate.postForObject(anyString(), anyString(), eq(String.class))).thenReturn(successXml);

        Map<String, Object> result = wechatPayService.createOrder(
                "PAY123456",
                new BigDecimal("99.00"),
                "oTestOpenId",
                "127.0.0.1",
                "https://api.tailoris.com/api/v1/payment/wechat/callback",
                "测试商品"
        );

        assertNotNull(result);
        assertEquals("wx201410272009395522657a690389285100", result.get("prepay_id"));
        assertEquals("PAY123456", result.get("out_trade_no"));
        assertNotNull(result.get("paySign"));
        assertNotNull(result.get("timeStamp"));
        assertNotNull(result.get("nonce_str"));
    }

    @Test
    @DisplayName("创建微信支付订单-Redis锁冲突")
    void testCreateOrder_LockConflict() {
        when(valueOperations.setIfAbsent(anyString(), eq("LOCKED"), eq(5L), eq(TimeUnit.MINUTES))).thenReturn(false);

        BusinessException exception = assertThrows(BusinessException.class, () ->
                wechatPayService.createOrder(
                        "PAY123456",
                        new BigDecimal("99.00"),
                        "oTestOpenId",
                        "127.0.0.1",
                        null,
                        "测试商品"
                ));

        assertTrue(exception.getMessage().contains("处理中"));
    }

    @Test
    @DisplayName("创建微信支付订单-返回失败return_code")
    void testCreateOrder_FailReturnCode() {
        when(valueOperations.setIfAbsent(anyString(), eq("LOCKED"), eq(5L), eq(TimeUnit.MINUTES))).thenReturn(true);
        when(stringRedisTemplate.delete(anyString())).thenReturn(true);

        String failXml = "<xml>" +
                "<return_code>FAIL</return_code>" +
                "<return_msg>签名失败</return_msg>" +
                "</xml>";
        when(restTemplate.postForObject(anyString(), anyString(), eq(String.class))).thenReturn(failXml);

        BusinessException exception = assertThrows(BusinessException.class, () ->
                wechatPayService.createOrder(
                        "PAY123456",
                        new BigDecimal("99.00"),
                        "oTestOpenId",
                        "127.0.0.1",
                        null,
                        "测试商品"
                ));

        assertTrue(exception.getMessage().contains("下单失败"));
    }

    @Test
    @DisplayName("创建微信支付订单-返回失败result_code")
    void testCreateOrder_FailResultCode() {
        when(valueOperations.setIfAbsent(anyString(), eq("LOCKED"), eq(5L), eq(TimeUnit.MINUTES))).thenReturn(true);
        when(stringRedisTemplate.delete(anyString())).thenReturn(true);

        String failXml = "<xml>" +
                "<return_code>SUCCESS</return_code>" +
                "<result_code>FAIL</result_code>" +
                "<err_code_des>余额不足</err_code_des>" +
                "</xml>";
        when(restTemplate.postForObject(anyString(), anyString(), eq(String.class))).thenReturn(failXml);

        BusinessException exception = assertThrows(BusinessException.class, () ->
                wechatPayService.createOrder(
                        "PAY123456",
                        new BigDecimal("99.00"),
                        "oTestOpenId",
                        "127.0.0.1",
                        null,
                        "测试商品"
                ));

        assertTrue(exception.getMessage().contains("下单失败"));
    }

    @Test
    @DisplayName("创建微信支付订单-使用默认notifyUrl")
    void testCreateOrder_DefaultNotifyUrl() {
        when(valueOperations.setIfAbsent(anyString(), eq("LOCKED"), eq(5L), eq(TimeUnit.MINUTES))).thenReturn(true);
        when(stringRedisTemplate.delete(anyString())).thenReturn(true);

        String successXml = "<xml>" +
                "<return_code>SUCCESS</return_code>" +
                "<result_code>SUCCESS</result_code>" +
                "<prepay_id>wx_prepay_123</prepay_id>" +
                "</xml>";
        when(restTemplate.postForObject(anyString(), anyString(), eq(String.class))).thenReturn(successXml);

        Map<String, Object> result = wechatPayService.createOrder(
                "PAY123456",
                new BigDecimal("99.00"),
                "oTestOpenId",
                "127.0.0.1",
                null,
                "测试商品"
        );

        assertNotNull(result);
    }

    @Test
    @DisplayName("查询微信订单-通过transaction_id")
    void testQueryOrder_ByTransactionId() {
        String responseXml = "<xml>" +
                "<return_code>SUCCESS</return_code>" +
                "<trade_state>SUCCESS</trade_state>" +
                "<transaction_id>TXN123456</transaction_id>" +
                "<out_trade_no>PAY123456</out_trade_no>" +
                "<total_fee>9900</total_fee>" +
                "</xml>";
        when(restTemplate.postForObject(anyString(), anyString(), eq(String.class))).thenReturn(responseXml);

        Map<String, Object> result = wechatPayService.queryOrder("TXN123456", null);

        assertNotNull(result);
        assertEquals("SUCCESS", result.get("trade_state"));
        assertEquals("TXN123456", result.get("transaction_id"));
    }

    @Test
    @DisplayName("查询微信订单-通过out_trade_no")
    void testQueryOrder_ByOutTradeNo() {
        String responseXml = "<xml>" +
                "<return_code>SUCCESS</return_code>" +
                "<trade_state>SUCCESS</trade_state>" +
                "<out_trade_no>PAY123456</out_trade_no>" +
                "</xml>";
        when(restTemplate.postForObject(anyString(), anyString(), eq(String.class))).thenReturn(responseXml);

        Map<String, Object> result = wechatPayService.queryOrder(null, "PAY123456");

        assertNotNull(result);
        assertEquals("SUCCESS", result.get("trade_state"));
    }

    @Test
    @DisplayName("查询微信订单-缺少必要参数")
    void testQueryOrder_MissingParams() {
        assertThrows(BusinessException.class, () ->
                wechatPayService.queryOrder(null, null));
    }

    @Test
    @DisplayName("微信退款-成功")
    void testRefund_Success() {
        String responseXml = "<xml>" +
                "<return_code>SUCCESS</return_code>" +
                "<result_code>SUCCESS</result_code>" +
                "<refund_id>REF123456</refund_id>" +
                "<out_refund_no>REF789</out_refund_no>" +
                "<refund_fee>5000</refund_fee>" +
                "</xml>";
        when(restTemplate.postForObject(anyString(), anyString(), eq(String.class))).thenReturn(responseXml);

        Map<String, Object> result = wechatPayService.refund(
                "PAY123456",
                "REF789",
                new BigDecimal("50.00"),
                new BigDecimal("100.00")
        );

        assertNotNull(result);
        assertEquals("REF123456", result.get("refund_id"));
    }

    @Test
    @DisplayName("验证回调签名-成功")
    void testVerifyCallback_Success() {
        Map<String, String> params = new HashMap<>();
        params.put("appid", "wx_test_appid");
        params.put("mch_id", "1234567890");
        params.put("result_code", "SUCCESS");
        params.put("sign_type", "MD5");

        // 先生成正确的签名
        String sign = generateTestSign(params);
        params.put("sign", sign);

        boolean result = wechatPayService.verifyCallback(params);
        assertTrue(result);
    }

    @Test
    @DisplayName("验证回调签名-签名错误")
    void testVerifyCallback_WrongSign() {
        Map<String, String> params = new HashMap<>();
        params.put("appid", "wx_test_appid");
        params.put("mch_id", "1234567890");
        params.put("result_code", "SUCCESS");
        params.put("sign_type", "MD5");
        params.put("sign", "WRONG_SIGN");

        boolean result = wechatPayService.verifyCallback(params);
        assertFalse(result);
    }

    @Test
    @DisplayName("验证回调签名-无sign字段")
    void testVerifyCallback_NoSign() {
        Map<String, String> params = new HashMap<>();
        params.put("appid", "wx_test_appid");
        params.put("mch_id", "1234567890");
        params.put("result_code", "SUCCESS");

        boolean result = wechatPayService.verifyCallback(params);
        assertFalse(result);
    }

    @Test
    @DisplayName("验证回调签名-默认MD5类型")
    void testVerifyCallback_DefaultSignType() {
        Map<String, String> params = new HashMap<>();
        params.put("appid", "wx_test_appid");
        params.put("mch_id", "1234567890");
        params.put("result_code", "SUCCESS");

        String sign = generateTestSign(params);
        params.put("sign", sign);

        boolean result = wechatPayService.verifyCallback(params);
        assertTrue(result);
    }

    private String generateTestSign(Map<String, String> params) {
        java.util.List<String> keys = new java.util.ArrayList<>(params.keySet());
        java.util.Collections.sort(keys);

        StringBuilder sb = new StringBuilder();
        for (String key : keys) {
            String value = params.get(key);
            if (value != null && !value.isEmpty() && !"sign".equals(key)) {
                sb.append(key).append("=").append(value).append("&");
            }
        }
        sb.append("key=").append("test_api_key_1234567890123456");

        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] bytes = md.digest(sb.toString().getBytes());
            StringBuilder hex = new StringBuilder();
            for (byte b : bytes) {
                String h = Integer.toHexString(b & 0xFF);
                if (h.length() == 1) hex.append("0");
                hex.append(h);
            }
            return hex.toString().toUpperCase();
        } catch (Exception e) {
            return "";
        }
    }
}
