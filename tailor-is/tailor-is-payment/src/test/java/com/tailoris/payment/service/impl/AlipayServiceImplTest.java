package com.tailoris.payment.service.impl;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.request.AlipayTradeRefundRequest;
import com.alipay.api.response.AlipayTradePagePayResponse;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.alipay.api.response.AlipayTradeRefundResponse;
import com.tailoris.common.exception.BusinessException;
import com.tailoris.payment.config.AlipayConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("支付宝服务测试")
class AlipayServiceImplTest {

    @Mock
    private AlipayConfig alipayConfig;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations valueOperations;

    private AlipayServiceImpl alipayService;

    @BeforeEach
    void setUp() {
        alipayService = new AlipayServiceImpl(alipayConfig, stringRedisTemplate);
        
        lenient().when(alipayConfig.getGatewayUrl()).thenReturn("https://openapi.alipay.com/gateway.do");
        lenient().when(alipayConfig.getAppId()).thenReturn("2021001234567890");
        lenient().when(alipayConfig.getPrivateKey()).thenReturn("MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQC");
        lenient().when(alipayConfig.getPublicKey()).thenReturn("MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA");
        lenient().when(alipayConfig.getCharset()).thenReturn("UTF-8");
        lenient().when(alipayConfig.getSignType()).thenReturn("RSA2");
        lenient().when(alipayConfig.getReturnUrl()).thenReturn("https://merchant.com/return");
        lenient().when(alipayConfig.getNotifyUrl()).thenReturn("https://merchant.com/notify");
        
        lenient().when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    @DisplayName("创建支付宝订单-获取锁失败")
    void testCreateOrder_LockFailed() {
        when(valueOperations.setIfAbsent(anyString(), eq("LOCKED"), eq(5L), eq(TimeUnit.MINUTES)))
                .thenReturn(false);

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            alipayService.createOrder(
                    "PAY123456",
                    new BigDecimal("100.00"),
                    "测试商品",
                    "商品描述",
                    null,
                    null
            );
        });

        assertEquals("订单正在处理中，请稍后", exception.getMessage());
        verify(stringRedisTemplate, never()).delete(anyString());
    }

    @Test
    @DisplayName("创建支付宝订单-API调用失败")
    void testCreateOrder_ApiFailed() {
        when(valueOperations.setIfAbsent(anyString(), eq("LOCKED"), eq(5L), eq(TimeUnit.MINUTES)))
                .thenReturn(true);
        when(stringRedisTemplate.delete(anyString())).thenReturn(true);

        try (MockedConstruction<DefaultAlipayClient> mocked = mockConstruction(DefaultAlipayClient.class,
                (mock, context) -> {
                    try {
                        when(mock.pageExecute(any(AlipayTradePagePayRequest.class)))
                                .thenThrow(new AlipayApiException("网络连接失败"));
                    } catch (AlipayApiException e) {
                        throw new RuntimeException(e);
                    }
                })) {

            BusinessException exception = assertThrows(BusinessException.class, () -> {
                alipayService.createOrder(
                        "PAY123456",
                        new BigDecimal("100.00"),
                        "测试商品",
                        "商品描述",
                        null,
                        null
                );
            });

            assertTrue(exception.getMessage().contains("支付宝下单失败"));
            verify(stringRedisTemplate).delete(contains("PAY123456"));
        }
    }

    @Test
    @DisplayName("创建支付宝订单-响应失败")
    void testCreateOrder_ResponseFailed() {
        when(valueOperations.setIfAbsent(anyString(), eq("LOCKED"), eq(5L), eq(TimeUnit.MINUTES)))
                .thenReturn(true);
        when(stringRedisTemplate.delete(anyString())).thenReturn(true);

        try (MockedConstruction<DefaultAlipayClient> mocked = mockConstruction(DefaultAlipayClient.class,
                (mock, context) -> {
                    try {
                        AlipayTradePagePayResponse response = mock(AlipayTradePagePayResponse.class);
                        when(response.isSuccess()).thenReturn(false);
                        when(response.getSubMsg()).thenReturn("参数错误");
                        when(mock.pageExecute(any(AlipayTradePagePayRequest.class))).thenReturn(response);
                    } catch (AlipayApiException e) {
                        throw new RuntimeException(e);
                    }
                })) {

            BusinessException exception = assertThrows(BusinessException.class, () -> {
                alipayService.createOrder(
                        "PAY123456",
                        new BigDecimal("100.00"),
                        "测试商品",
                        "商品描述",
                        null,
                        null
                );
            });

            assertTrue(exception.getMessage().contains("支付宝下单失败"));
            assertTrue(exception.getMessage().contains("参数错误"));
        }
    }

    @Test
    @DisplayName("创建支付宝订单-成功")
    void testCreateOrder_Success() {
        when(valueOperations.setIfAbsent(anyString(), eq("LOCKED"), eq(5L), eq(TimeUnit.MINUTES)))
                .thenReturn(true);
        when(stringRedisTemplate.delete(anyString())).thenReturn(true);

        try (MockedConstruction<DefaultAlipayClient> mocked = mockConstruction(DefaultAlipayClient.class,
                (mock, context) -> {
                    try {
                        AlipayTradePagePayResponse response = mock(AlipayTradePagePayResponse.class);
                        when(response.isSuccess()).thenReturn(true);
                        when(response.getBody()).thenReturn("<form>支付宝支付表单</form>");
                        when(mock.pageExecute(any(AlipayTradePagePayRequest.class))).thenReturn(response);
                    } catch (AlipayApiException e) {
                        throw new RuntimeException(e);
                    }
                })) {

            String result = alipayService.createOrder(
                    "PAY123456",
                    new BigDecimal("100.00"),
                    "测试商品",
                    "商品描述",
                    "https://merchant.com/return",
                    "https://merchant.com/notify"
            );

            assertNotNull(result);
            assertEquals("<form>支付宝支付表单</form>", result);
            verify(stringRedisTemplate).delete(contains("PAY123456"));
        }
    }

    @Test
    @DisplayName("创建支付宝订单-使用默认回调地址")
    void testCreateOrder_DefaultCallbackUrl() {
        when(valueOperations.setIfAbsent(anyString(), eq("LOCKED"), eq(5L), eq(TimeUnit.MINUTES)))
                .thenReturn(true);
        when(stringRedisTemplate.delete(anyString())).thenReturn(true);

        try (MockedConstruction<DefaultAlipayClient> mocked = mockConstruction(DefaultAlipayClient.class,
                (mock, context) -> {
                    try {
                        AlipayTradePagePayResponse response = mock(AlipayTradePagePayResponse.class);
                        when(response.isSuccess()).thenReturn(true);
                        when(response.getBody()).thenReturn("<form>支付表单</form>");
                        when(mock.pageExecute(any(AlipayTradePagePayRequest.class))).thenReturn(response);
                    } catch (AlipayApiException e) {
                        throw new RuntimeException(e);
                    }
                })) {

            String result = alipayService.createOrder(
                    "PAY123456",
                    new BigDecimal("100.00"),
                    "测试商品",
                    "商品描述",
                    null,
                    null
            );

            assertNotNull(result);
            verify(stringRedisTemplate).delete(contains("PAY123456"));
        }
    }

    @Test
    @DisplayName("查询支付宝订单-缺少必要参数")
    void testQueryOrder_MissingParams() {
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            alipayService.queryOrder(null, null);
        });

        assertEquals("必须提供trade_no或out_trade_no", exception.getMessage());
    }

    @Test
    @DisplayName("查询支付宝订单-使用trade_no")
    void testQueryOrder_WithTradeNo() {
        try (MockedConstruction<DefaultAlipayClient> mocked = mockConstruction(DefaultAlipayClient.class,
                (mock, context) -> {
                    try {
                        AlipayTradeQueryResponse response = mock(AlipayTradeQueryResponse.class);
                        when(response.getCode()).thenReturn("10000");
                        when(response.getMsg()).thenReturn("Success");
                        when(response.getTradeNo()).thenReturn("2021xxx");
                        when(response.getOutTradeNo()).thenReturn("PAY123456");
                        when(response.getTradeStatus()).thenReturn("TRADE_SUCCESS");
                        when(response.getTotalAmount()).thenReturn("100.00");
                        when(response.getReceiptAmount()).thenReturn("100.00");
                        when(mock.execute(any(AlipayTradeQueryRequest.class))).thenReturn(response);
                    } catch (AlipayApiException e) {
                        throw new RuntimeException(e);
                    }
                })) {

            Map<String, Object> result = alipayService.queryOrder("2021xxx", null);

            assertNotNull(result);
            assertEquals("10000", result.get("code"));
            assertEquals("Success", result.get("msg"));
            assertEquals("2021xxx", result.get("trade_no"));
            assertEquals("PAY123456", result.get("out_trade_no"));
            assertEquals("TRADE_SUCCESS", result.get("trade_status"));
        }
    }

    @Test
    @DisplayName("查询支付宝订单-使用out_trade_no")
    void testQueryOrder_WithOutTradeNo() {
        try (MockedConstruction<DefaultAlipayClient> mocked = mockConstruction(DefaultAlipayClient.class,
                (mock, context) -> {
                    try {
                        AlipayTradeQueryResponse response = mock(AlipayTradeQueryResponse.class);
                        when(response.getCode()).thenReturn("10000");
                        when(response.getMsg()).thenReturn("Success");
                        when(response.getTradeNo()).thenReturn("2021xxx");
                        when(response.getOutTradeNo()).thenReturn("PAY123456");
                        when(response.getTradeStatus()).thenReturn("TRADE_SUCCESS");
                        when(response.getTotalAmount()).thenReturn("100.00");
                        when(response.getReceiptAmount()).thenReturn("100.00");
                        when(mock.execute(any(AlipayTradeQueryRequest.class))).thenReturn(response);
                    } catch (AlipayApiException e) {
                        throw new RuntimeException(e);
                    }
                })) {

            Map<String, Object> result = alipayService.queryOrder(null, "PAY123456");

            assertNotNull(result);
            assertEquals("10000", result.get("code"));
            assertEquals("PAY123456", result.get("out_trade_no"));
        }
    }

    @Test
    @DisplayName("查询支付宝订单-API异常")
    void testQueryOrder_ApiException() {
        try (MockedConstruction<DefaultAlipayClient> mocked = mockConstruction(DefaultAlipayClient.class,
                (mock, context) -> {
                    try {
                        when(mock.execute(any(AlipayTradeQueryRequest.class)))
                                .thenThrow(new AlipayApiException("查询失败"));
                    } catch (AlipayApiException e) {
                        throw new RuntimeException(e);
                    }
                })) {

            BusinessException exception = assertThrows(BusinessException.class, () -> {
                alipayService.queryOrder(null, "PAY123456");
            });

            assertTrue(exception.getMessage().contains("支付宝查询失败"));
        }
    }

    @Test
    @DisplayName("支付宝退款-成功")
    void testRefund_Success() {
        try (MockedConstruction<DefaultAlipayClient> mocked = mockConstruction(DefaultAlipayClient.class,
                (mock, context) -> {
                    try {
                        AlipayTradeRefundResponse response = mock(AlipayTradeRefundResponse.class);
                        when(response.isSuccess()).thenReturn(true);
                        when(response.getTradeNo()).thenReturn("2021xxx");
                        when(mock.execute(any(AlipayTradeRefundRequest.class))).thenReturn(response);
                    } catch (AlipayApiException e) {
                        throw new RuntimeException(e);
                    }
                })) {

            String result = alipayService.refund(
                    "PAY123456",
                    "REFUND001",
                    new BigDecimal("50.00"),
                    "用户申请退款"
            );

            assertEquals("2021xxx", result);
        }
    }

    @Test
    @DisplayName("支付宝退款-响应失败")
    void testRefund_ResponseFailed() {
        try (MockedConstruction<DefaultAlipayClient> mocked = mockConstruction(DefaultAlipayClient.class,
                (mock, context) -> {
                    try {
                        AlipayTradeRefundResponse response = mock(AlipayTradeRefundResponse.class);
                        when(response.isSuccess()).thenReturn(false);
                        when(response.getSubMsg()).thenReturn("余额不足");
                        when(mock.execute(any(AlipayTradeRefundRequest.class))).thenReturn(response);
                    } catch (AlipayApiException e) {
                        throw new RuntimeException(e);
                    }
                })) {

            BusinessException exception = assertThrows(BusinessException.class, () -> {
                alipayService.refund(
                        "PAY123456",
                        "REFUND001",
                        new BigDecimal("50.00"),
                        "用户申请退款"
                );
            });

            assertTrue(exception.getMessage().contains("支付宝退款失败"));
            assertTrue(exception.getMessage().contains("余额不足"));
        }
    }

    @Test
    @DisplayName("支付宝退款-API异常")
    void testRefund_ApiException() {
        try (MockedConstruction<DefaultAlipayClient> mocked = mockConstruction(DefaultAlipayClient.class,
                (mock, context) -> {
                    try {
                        when(mock.execute(any(AlipayTradeRefundRequest.class)))
                                .thenThrow(new AlipayApiException("退款失败"));
                    } catch (AlipayApiException e) {
                        throw new RuntimeException(e);
                    }
                })) {

            BusinessException exception = assertThrows(BusinessException.class, () -> {
                alipayService.refund(
                        "PAY123456",
                        "REFUND001",
                        new BigDecimal("50.00"),
                        null
                );
            });

            assertTrue(exception.getMessage().contains("支付宝退款失败"));
        }
    }

    @Test
    @DisplayName("支付宝退款-不传退款原因")
    void testRefund_WithoutReason() {
        try (MockedConstruction<DefaultAlipayClient> mocked = mockConstruction(DefaultAlipayClient.class,
                (mock, context) -> {
                    try {
                        AlipayTradeRefundResponse response = mock(AlipayTradeRefundResponse.class);
                        when(response.isSuccess()).thenReturn(true);
                        when(response.getTradeNo()).thenReturn("2021xxx");
                        when(mock.execute(any(AlipayTradeRefundRequest.class))).thenReturn(response);
                    } catch (AlipayApiException e) {
                        throw new RuntimeException(e);
                    }
                })) {

            String result = alipayService.refund(
                    "PAY123456",
                    "REFUND001",
                    new BigDecimal("50.00"),
                    null
            );

            assertEquals("2021xxx", result);
        }
    }

    @Test
    @DisplayName("验证回调-使用默认签名类型")
    void testVerifyCallback_DefaultSignType() {
        Map<String, String> params = new HashMap<>();
        params.put("trade_no", "2021xxx");
        params.put("out_trade_no", "PAY123456");
        params.put("sign", "xxx");

        // 由于AlipaySignature.rsaCheckV1是静态方法，这里只测试参数处理逻辑
        // 实际验签会失败，但会返回false而不是抛出异常
        boolean result = alipayService.verifyCallback(params);
        
        // 验签失败返回false
        assertFalse(result);
    }

    @Test
    @DisplayName("验证回调-使用指定签名类型")
    void testVerifyCallback_WithSignType() {
        Map<String, String> params = new HashMap<>();
        params.put("trade_no", "2021xxx");
        params.put("out_trade_no", "PAY123456");
        params.put("sign", "xxx");
        params.put("sign_type", "RSA");

        boolean result = alipayService.verifyCallback(params);
        
        // 验签失败返回false
        assertFalse(result);
    }
}
