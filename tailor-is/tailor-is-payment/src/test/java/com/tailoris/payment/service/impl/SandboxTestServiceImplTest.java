package com.tailoris.payment.service.impl;

import com.tailoris.payment.entity.PaymentRecord;
import com.tailoris.payment.service.AlipayService;
import com.tailoris.payment.service.PaymentService;
import com.tailoris.payment.service.WechatPayService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("SandboxTestServiceImpl 测试")
@ExtendWith(MockitoExtension.class)
class SandboxTestServiceImplTest {

    @Mock
    private WechatPayService wechatPayService;

    @Mock
    private AlipayService alipayService;

    @Mock
    private PaymentService paymentService;

    @InjectMocks
    private SandboxTestServiceImpl sandboxTestService;

    private static final String ORDER_NO = "ORD202606130001";
    private static final BigDecimal AMOUNT = new BigDecimal("0.01");

    @Test
    @DisplayName("微信支付沙箱测试 - 成功")
    void testWechatPay_Success() {
        Map<String, Object> payResult = new HashMap<>();
        payResult.put("prepay_id", "wx_prepay_123");
        when(wechatPayService.createJsapiPayment(anyString(), any(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(payResult);

        Map<String, Object> result = sandboxTestService.testWechatPay(ORDER_NO, AMOUNT, null);

        assertTrue((Boolean) result.get("success"));
        assertEquals("微信支付下单成功", result.get("message"));
        assertEquals(payResult, result.get("data"));
    }

    @Test
    @DisplayName("微信支付沙箱测试 - 失败")
    void testWechatPay_Failure() {
        when(wechatPayService.createJsapiPayment(anyString(), any(), anyString(), anyString(), anyString(), anyString()))
                .thenThrow(new RuntimeException("网络异常"));

        Map<String, Object> result = sandboxTestService.testWechatPay(ORDER_NO, AMOUNT, null);

        assertFalse((Boolean) result.get("success"));
        assertTrue(((String) result.get("message")).contains("网络异常"));
    }

    @Test
    @DisplayName("支付宝沙箱测试 - 成功")
    void testAlipay_Success() {
        when(alipayService.createOrder(anyString(), any(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn("<form>alipay form</form>");

        Map<String, Object> result = sandboxTestService.testAlipay(ORDER_NO, AMOUNT, null);

        assertTrue((Boolean) result.get("success"));
        assertEquals("支付宝下单成功", result.get("message"));
        assertEquals("<form>alipay form</form>", result.get("form"));
    }

    @Test
    @DisplayName("支付宝沙箱测试 - 失败")
    void testAlipay_Failure() {
        when(alipayService.createOrder(anyString(), any(), anyString(), anyString(), anyString(), anyString()))
                .thenThrow(new RuntimeException("服务异常"));

        Map<String, Object> result = sandboxTestService.testAlipay(ORDER_NO, AMOUNT, null);

        assertFalse((Boolean) result.get("success"));
        assertTrue(((String) result.get("message")).contains("服务异常"));
    }

    @Test
    @DisplayName("验证支付状态 - 支付记录存在")
    void verifyPaymentStatus_PaymentExists() {
        PaymentRecord record = new PaymentRecord();
        record.setPaymentNo("PAY202606130001");
        record.setPayStatus(2);
        record.setAmount(new BigDecimal("100.00"));
        when(paymentService.getPaymentByPaymentNo("PAY202606130001")).thenReturn(record);

        Map<String, Object> result = sandboxTestService.verifyPaymentStatus("PAY202606130001");

        assertTrue((Boolean) result.get("success"));
        assertEquals("PAY202606130001", result.get("paymentNo"));
        assertEquals("已支付", result.get("payStatus"));
    }

    @Test
    @DisplayName("验证支付状态 - 支付记录不存在")
    void verifyPaymentStatus_PaymentNotFound() {
        when(paymentService.getPaymentByPaymentNo("PAY202606130001")).thenReturn(null);

        Map<String, Object> result = sandboxTestService.verifyPaymentStatus("PAY202606130001");

        assertFalse((Boolean) result.get("success"));
        assertEquals("支付记录不存在", result.get("message"));
    }

    @Test
    @DisplayName("验证支付状态 - 服务异常")
    void verifyPaymentStatus_Exception() {
        when(paymentService.getPaymentByPaymentNo("PAY202606130001"))
                .thenThrow(new RuntimeException("数据库异常"));

        Map<String, Object> result = sandboxTestService.verifyPaymentStatus("PAY202606130001");

        assertFalse((Boolean) result.get("success"));
        assertTrue(((String) result.get("message")).contains("数据库异常"));
    }

    @Test
    @DisplayName("微信退款沙箱测试 - 成功")
    void testWechatRefund_Success() {
        Map<String, Object> refundResult = new HashMap<>();
        refundResult.put("refund_id", "wx_refund_123");
        when(wechatPayService.createRefund(anyString(), anyString(), any(), any(), any())).thenReturn(refundResult);

        Map<String, Object> result = sandboxTestService.testWechatRefund(ORDER_NO, AMOUNT);

        assertTrue((Boolean) result.get("success"));
        assertEquals("微信退款成功", result.get("message"));
    }

    @Test
    @DisplayName("支付宝退款沙箱测试 - 成功")
    void testAlipayRefund_Success() {
        when(alipayService.refund(anyString(), anyString(), any(), anyString())).thenReturn("trade_123");

        Map<String, Object> result = sandboxTestService.testAlipayRefund(ORDER_NO, AMOUNT);

        assertTrue((Boolean) result.get("success"));
        assertEquals("支付宝退款成功", result.get("message"));
        assertEquals("trade_123", result.get("tradeNo"));
    }
}