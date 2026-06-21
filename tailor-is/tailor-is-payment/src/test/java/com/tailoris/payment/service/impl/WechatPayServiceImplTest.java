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

        Map<String, Object> result = wechatPayService.createJsapiPayment(
                "PAY123456",
                new BigDecimal("99.00"),
                "oTestOpenId",
                "测试商品",
                "127.0.0.1",
                "https://api.tailoris.com/api/v1/payment/wechat/callback"
        );

        assertNotNull(result);
    }

    @Test
    @DisplayName("创建微信支付订单-Redis锁冲突")
    void testCreateOrder_LockConflict() {
        when(valueOperations.setIfAbsent(anyString(), eq("LOCKED"), eq(5L), eq(TimeUnit.MINUTES))).thenReturn(false);

        BusinessException exception = assertThrows(BusinessException.class, () ->
                wechatPayService.createJsapiPayment(
                        "PAY123456",
                        new BigDecimal("99.00"),
                        "oTestOpenId",
                        "测试商品",
                        "127.0.0.1",
                        null
                ));

        assertTrue(exception.getMessage().contains("处理中"));
    }

    @Test
    @DisplayName("创建微信支付订单-使用默认notifyUrl")
    void testCreateOrder_DefaultNotifyUrl() {
        when(valueOperations.setIfAbsent(anyString(), eq("LOCKED"), eq(5L), eq(TimeUnit.MINUTES))).thenReturn(true);
        when(stringRedisTemplate.delete(anyString())).thenReturn(true);

        Map<String, Object> result = wechatPayService.createJsapiPayment(
                "PAY123456",
                new BigDecimal("99.00"),
                "oTestOpenId",
                "测试商品",
                "127.0.0.1",
                null
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

        Map<String, Object> result = wechatPayService.queryPayment("TXN123456", null);

        assertNotNull(result);
    }

    @Test
    @DisplayName("查询微信订单-通过out_trade_no")
    void testQueryOrder_ByOutTradeNo() {
        Map<String, Object> result = wechatPayService.queryPayment(null, "PAY123456");

        assertNotNull(result);
    }

    @Test
    @DisplayName("查询微信订单-缺少必要参数")
    void testQueryOrder_MissingParams() {
        assertThrows(BusinessException.class, () ->
                wechatPayService.queryPayment(null, null));
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

        Map<String, Object> result = wechatPayService.createRefund(
                "PAY123456",
                "REF789",
                new BigDecimal("50.00"),
                new BigDecimal("100.00"),
                "测试退款"
        );

        assertNotNull(result);
    }

    @Test
    @DisplayName("验证回调签名-成功")
    void testVerifyCallback_Success() {
        String body = "{\"out_trade_no\":\"PAY123456\"}";
        boolean result = wechatPayService.verifyCallback(body, "VALID_SIGN", "1234567890", "nonce123", "SERIAL001");
        assertTrue(result);
    }

    @Test
    @DisplayName("验证回调签名-签名错误")
    void testVerifyCallback_WrongSign() {
        String body = "{\"out_trade_no\":\"PAY123456\"}";
        boolean result = wechatPayService.verifyCallback(body, "WRONG_SIGN", "1234567890", "nonce123", "SERIAL001");
        assertFalse(result);
    }

    @Test
    @DisplayName("验证回调签名-无sign字段")
    void testVerifyCallback_NoSign() {
        String body = "{\"out_trade_no\":\"PAY123456\"}";
        boolean result = wechatPayService.verifyCallback(body, null, "1234567890", "nonce123", "SERIAL001");
        assertFalse(result);
    }

    @Test
    @DisplayName("验证回调签名-默认MD5类型")
    void testVerifyCallback_DefaultSignType() {
        String body = "{\"out_trade_no\":\"PAY123456\"}";
        boolean result = wechatPayService.verifyCallback(body, "VALID_SIGN", "1234567890", "nonce123", "SERIAL001");
        assertTrue(result);
    }
}
