package com.tailoris.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tailoris.common.exception.BusinessException;
import com.tailoris.payment.dto.PayRequest;
import com.tailoris.payment.entity.PaymentRecord;
import com.tailoris.payment.entity.RefundRecord;
import com.tailoris.payment.mapper.PaymentRecordMapper;
import com.tailoris.payment.mapper.RefundRecordMapper;
import com.tailoris.payment.mapper.MerchantAccountMapper;
import com.tailoris.payment.mapper.UserAccountMapper;
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

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * PaymentServiceImpl 单元测试.
 *
 * <p>TODO 待补充测试场景：</p>
 * <ul>
 *   <li>T-M05: 支付宝/微信支付回调处理测试</li>
 *   <li>T-M05: 退款流程完整测试（含部分退款）</li>
 *   <li>T-M05: 支付超时关单测试</li>
 *   <li>T-M05: 账户余额变动/冻结测试</li>
 *   <li>T-M05: 对账差异处理测试</li>
 *   <li>T-M05: 分布式事务一致性测试</li>
 * </ul>
 */

@DisplayName("PaymentServiceImpl 单元测试")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PaymentServiceImplTest {

    @Mock
    private PaymentRecordMapper paymentRecordMapper;

    @Mock
    private RefundRecordMapper refundRecordMapper;

    @Mock
    private MerchantAccountMapper merchantAccountMapper;

    @Mock
    private UserAccountMapper userAccountMapper;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private EscrowServiceImpl escrowService;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    @BeforeEach
    void setUp() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    @DisplayName("创建支付成功")
    void testPay_Success() {
        when(valueOperations.setIfAbsent(anyString(), eq("1"), eq(30L), eq(TimeUnit.SECONDS))).thenReturn(true);
        when(paymentRecordMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(paymentRecordMapper.insert(any(PaymentRecord.class))).thenReturn(1);

        PayRequest request = new PayRequest();
        request.setOrderId(1001L);
        request.setAmount(new BigDecimal("99.00"));
        request.setPayChannel(1);
        request.setPayMethod("WECHAT");

        PaymentRecord result = paymentService.createPayment(1L, request);

        assertNotNull(result);
        assertNotNull(result.getPaymentNo());
        assertEquals(1001L, result.getOrderId());
        assertEquals(0, result.getPayStatus());

        verify(paymentRecordMapper).insert(any(PaymentRecord.class));
    }

    @Test
    @DisplayName("支付时Redis锁冲突")
    void testPay_RedisLockConflict() {
        when(valueOperations.setIfAbsent(anyString(), eq("1"), eq(30L), eq(TimeUnit.SECONDS))).thenReturn(false);

        PayRequest request = new PayRequest();
        request.setOrderId(1001L);
        request.setAmount(new BigDecimal("99.00"));

        assertThrows(BusinessException.class, () -> paymentService.createPayment(1L, request));
    }

    @Test
    @DisplayName("支付时订单已有待支付记录")
    void testPay_OrderAlreadyPending() {
        when(valueOperations.setIfAbsent(anyString(), eq("1"), eq(30L), eq(TimeUnit.SECONDS))).thenReturn(true);
        when(paymentRecordMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        PayRequest request = new PayRequest();
        request.setOrderId(1001L);
        request.setAmount(new BigDecimal("99.00"));

        assertThrows(BusinessException.class, () -> paymentService.createPayment(1L, request));
    }

    @Test
    @DisplayName("退款成功")
    void testRefund_Success() {
        when(refundRecordMapper.insert(any(RefundRecord.class))).thenReturn(1);

        RefundRecord result = paymentService.refund(1L, 2001L, 1001L, new BigDecimal("99.00"), 1, "测试退款");

        assertNotNull(result);
        assertEquals(0, result.getRefundStatus());
        assertNotNull(result.getRefundNo());

        verify(refundRecordMapper).insert(any(RefundRecord.class));
    }

    @Test
    @DisplayName("支付回调时支付记录不存在")
    void testPayCallback_RecordNotFound() {
        when(paymentRecordMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        assertThrows(BusinessException.class, () ->
                paymentService.payCallback("PAY_NOT_EXIST", "TXN123", "{}", 1L));
    }

    @Test
    @DisplayName("支付回调成功")
    void testPayCallback_Success() {
        PaymentRecord record = new PaymentRecord();
        record.setId(1L);
        record.setPaymentNo("PAY123456");
        record.setAmount(new BigDecimal("99.00"));
        record.setPayStatus(0);

        when(valueOperations.setIfAbsent(anyString(), eq("PROCESSING"), eq(5L), eq(TimeUnit.MINUTES))).thenReturn(true);
        when(paymentRecordMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(record);
        when(paymentRecordMapper.updateById(any(PaymentRecord.class))).thenReturn(1);

        assertDoesNotThrow(() ->
                paymentService.payCallback("PAY123456", "TXN123456", "{}", 1L));

        verify(paymentRecordMapper).updateById((PaymentRecord) argThat(r -> {
                PaymentRecord pr = (PaymentRecord) r;
                return pr.getPayStatus() == 2 && pr.getTransactionId().equals("TXN123456");
        }));
    }

    @Test
    @DisplayName("支付回调幂等性-重复回调")
    void testPayCallback_Idempotent() {
        PaymentRecord record = new PaymentRecord();
        record.setId(1L);
        record.setPaymentNo("PAY123456");
        record.setPayStatus(2);

        when(valueOperations.setIfAbsent(anyString(), eq("PROCESSING"), eq(5L), eq(TimeUnit.MINUTES))).thenReturn(true);
        when(paymentRecordMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(record);

        assertDoesNotThrow(() ->
                paymentService.payCallback("PAY123456", "TXN123456", "{}", 1L));

        verify(paymentRecordMapper, never()).updateById(any(PaymentRecord.class));
    }

    @Test
    @DisplayName("支付回调幂等性-处理中")
    void testPayCallback_Processing() {
        when(valueOperations.setIfAbsent(anyString(), eq("PROCESSING"), eq(5L), eq(TimeUnit.MINUTES))).thenReturn(false);
        when(valueOperations.get(anyString())).thenReturn("PROCESSING");

        BusinessException exception = assertThrows(BusinessException.class, () ->
                paymentService.payCallback("PAY123456", "TXN123456", "{}", 1L));
        assertTrue(exception.getMessage().contains("处理中"));
    }

    @Test
    @DisplayName("获取支付状态成功")
    void testGetPaymentStatus_Success() {
        PaymentRecord record = new PaymentRecord();
        record.setId(1L);
        record.setPaymentNo("PAY123456");
        record.setPayStatus(2);

        when(paymentRecordMapper.selectById(1L)).thenReturn(record);

        PaymentRecord result = paymentService.getPaymentStatus(1L);

        assertNotNull(result);
        assertEquals("PAY123456", result.getPaymentNo());
        assertEquals(2, result.getPayStatus());
    }

    @Test
    @DisplayName("获取支付状态-缓存命中")
    void testGetPaymentStatus_CacheHit() {
        PaymentRecord record = new PaymentRecord();
        record.setId(1L);
        record.setPaymentNo("PAY123456");

        when(valueOperations.get("payment:info:1")).thenReturn("cached");
        when(paymentRecordMapper.selectById(1L)).thenReturn(record);

        PaymentRecord result = paymentService.getPaymentStatus(1L);

        assertNotNull(result);
        assertEquals("PAY123456", result.getPaymentNo());
    }

    @Test
    @DisplayName("根据支付编号获取支付记录")
    void testGetPaymentByPaymentNo_Success() {
        PaymentRecord record = new PaymentRecord();
        record.setId(1L);
        record.setPaymentNo("PAY123456");

        when(paymentRecordMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(record);

        PaymentRecord result = paymentService.getPaymentByPaymentNo("PAY123456");

        assertNotNull(result);
        assertEquals("PAY123456", result.getPaymentNo());
    }

    @Test
    @DisplayName("根据支付编号获取支付记录-不存在")
    void testGetPaymentByPaymentNo_NotFound() {
        when(paymentRecordMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        PaymentRecord result = paymentService.getPaymentByPaymentNo("PAY_NOT_EXIST");

        assertNull(result);
    }

    @Test
    @DisplayName("创建支付成功-带可选参数")
    void testPay_WithOptionalParams() {
        when(valueOperations.setIfAbsent(anyString(), eq("1"), eq(30L), eq(TimeUnit.SECONDS))).thenReturn(true);
        when(paymentRecordMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(paymentRecordMapper.insert(any(PaymentRecord.class))).thenReturn(1);

        PayRequest request = new PayRequest();
        request.setOrderId(1002L);
        request.setAmount(new BigDecimal("150.00"));
        request.setPayChannel(2);
        request.setPayMethod("ALIPAY");
        request.setNotifyUrl("https://example.com/notify");
        request.setDeviceType(1);
        request.setRemark("测试订单备注");

        PaymentRecord result = paymentService.createPayment(2L, request);

        assertNotNull(result);
        assertEquals(1002L, result.getOrderId());
        assertEquals(new BigDecimal("150.00"), result.getAmount());
        assertEquals("ALIPAY", result.getPayMethod());
        verify(paymentRecordMapper).insert(any(PaymentRecord.class));
    }

    @Test
    @DisplayName("退款金额为零成功")
    void testRefund_ZeroAmount() {
        when(refundRecordMapper.insert(any(RefundRecord.class))).thenReturn(1);

        RefundRecord result = paymentService.refund(1L, 2001L, 1001L, BigDecimal.ZERO, 1, "零元退款");

        assertNotNull(result);
        assertEquals(BigDecimal.ZERO, result.getAmount());
        assertEquals(0, result.getRefundStatus());
    }

    @Test
    @DisplayName("获取支付状态-记录不存在返回null")
    void testGetPaymentStatus_RecordNotFound() {
        when(valueOperations.get("payment:info:999")).thenReturn(null);
        when(paymentRecordMapper.selectById(999L)).thenReturn(null);

        PaymentRecord result = paymentService.getPaymentStatus(999L);

        assertNull(result);
    }
}