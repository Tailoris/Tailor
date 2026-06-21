package com.tailoris.payment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tailoris.common.exception.BusinessException;
import com.tailoris.payment.dto.PayRequest;
import com.tailoris.payment.entity.PaymentRecord;
import com.tailoris.payment.entity.RefundRecord;
import com.tailoris.payment.mapper.PaymentRecordMapper;
import com.tailoris.payment.mapper.RefundRecordMapper;
import com.tailoris.payment.service.impl.PaymentServiceImpl;
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
 * PaymentService 补充单元测试 - TEST-P2-01.
 *
 * <p>覆盖 PaymentServiceImplTest 中 TODO 待补充的场景：</p>
 * <ul>
 *   <li>退款流程完整测试（全额退款、参数校验）</li>
 *   <li>支付回调幂等性测试</li>
 *   <li>支付状态查询测试</li>
 *   <li>支付创建重复提交测试</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("PaymentService 补充单元测试")
class PaymentServiceTest {

    @Mock private PaymentRecordMapper paymentRecordMapper;
    @Mock private RefundRecordMapper refundRecordMapper;
    @Mock private com.tailoris.payment.mapper.MerchantAccountMapper merchantAccountMapper;
    @Mock private com.tailoris.payment.mapper.UserAccountMapper userAccountMapper;
    @Mock private StringRedisTemplate stringRedisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;
    @Mock private com.tailoris.payment.service.impl.EscrowServiceImpl escrowService;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    @BeforeEach
    void setUp() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    // ============================================================
    // 退款流程
    // ============================================================

    @Test
    @DisplayName("退款 - 成功")
    void refund_Success() {
        Long userId = 1L;
        Long ticketId = 100L;
        Long orderId = 1001L;
        BigDecimal refundAmount = new BigDecimal("99.00");

        PaymentRecord payment = buildPaymentRecord(1L, "PAY001", new BigDecimal("99.00"), 2);
        when(paymentRecordMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(payment);
        when(refundRecordMapper.insert(any(RefundRecord.class))).thenReturn(1);
        when(paymentRecordMapper.updateById(any(PaymentRecord.class))).thenReturn(1);

        RefundRecord result = paymentService.refund(userId, ticketId, orderId, refundAmount, 1, "用户申请退款");

        assertNotNull(result);
        verify(refundRecordMapper).insert(any(RefundRecord.class));
    }

    @Test
    @DisplayName("退款 - 支付记录不存在")
    void refund_PaymentNotFound() {
        when(paymentRecordMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        assertThrows(BusinessException.class,
                () -> paymentService.refund(1L, 100L, 999L, new BigDecimal("50.00"), 1, "退款"));
        verify(refundRecordMapper, never()).insert(any(RefundRecord.class));
    }

    @Test
    @DisplayName("退款 - 支付记录未支付")
    void refund_NotPaid() {
        PaymentRecord payment = buildPaymentRecord(1L, "PAY001", new BigDecimal("100.00"), 0);
        when(paymentRecordMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(payment);

        assertThrows(BusinessException.class,
                () -> paymentService.refund(1L, 100L, 1001L, new BigDecimal("50.00"), 1, "未支付"));
    }

    // ============================================================
    // 支付回调
    // ============================================================

    @Test
    @DisplayName("支付回调 - 幂等性（已支付记录直接返回）")
    void payCallback_AlreadyPaid() {
        String paymentNo = "PAY001";
        PaymentRecord payment = buildPaymentRecord(1L, paymentNo, new BigDecimal("99.00"), 2);
        when(valueOperations.setIfAbsent(anyString(), eq("1"), eq(24L), eq(TimeUnit.HOURS))).thenReturn(true);
        when(paymentRecordMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(payment);

        assertDoesNotThrow(() -> paymentService.payCallback(paymentNo, "TXN001", "{}", "sign", "RSA"));
        // 已支付记录不更新
        verify(paymentRecordMapper, never()).updateById(any(PaymentRecord.class));
    }

    @Test
    @DisplayName("支付回调 - 支付记录不存在")
    void payCallback_NotFound() {
        String paymentNo = "PAY_NOT_EXIST";
        when(valueOperations.setIfAbsent(anyString(), eq("1"), eq(24L), eq(TimeUnit.HOURS))).thenReturn(true);
        when(paymentRecordMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        assertThrows(BusinessException.class,
                () -> paymentService.payCallback(paymentNo, "TXN001", "{}", "sign", "RSA"));
    }

    // ============================================================
    // 支付状态查询
    // ============================================================

    @Test
    @DisplayName("查询支付状态 - 缓存命中")
    void getPaymentStatus_CacheHit() {
        Long paymentId = 1L;
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("payment:status:" + paymentId)).thenReturn("2");

        PaymentRecord result = paymentService.getPaymentStatus(paymentId);

        assertNotNull(result);
        assertEquals(paymentId, result.getId());
        assertEquals(2, result.getPayStatus());
        // 缓存命中不查数据库
        verify(paymentRecordMapper, never()).selectById(anyLong());
    }

    @Test
    @DisplayName("查询支付状态 - 缓存未命中查数据库")
    void getPaymentStatus_CacheMiss() {
        Long paymentId = 1L;
        PaymentRecord payment = buildPaymentRecord(paymentId, "PAY001", new BigDecimal("99.00"), 2);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("payment:status:" + paymentId)).thenReturn(null);
        when(paymentRecordMapper.selectById(paymentId)).thenReturn(payment);
        when(valueOperations.set(anyString(), anyString(), eq(30L), eq(TimeUnit.MINUTES))).thenAnswer(inv -> null);

        PaymentRecord result = paymentService.getPaymentStatus(paymentId);

        assertNotNull(result);
        assertEquals(paymentId, result.getId());
        verify(paymentRecordMapper).selectById(paymentId);
    }

    @Test
    @DisplayName("根据支付单号查询支付记录")
    void getPaymentByPaymentNo_Success() {
        String paymentNo = "PAY001";
        PaymentRecord payment = buildPaymentRecord(1L, paymentNo, new BigDecimal("99.00"), 2);
        when(paymentRecordMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(payment);

        PaymentRecord result = paymentService.getPaymentByPaymentNo(paymentNo);

        assertNotNull(result);
        assertEquals(paymentNo, result.getPaymentNo());
    }

    @Test
    @DisplayName("根据支付单号查询 - 不存在")
    void getPaymentByPaymentNo_NotFound() {
        when(paymentRecordMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        PaymentRecord result = paymentService.getPaymentByPaymentNo("PAY_NOT_EXIST");

        assertNull(result);
    }

    // ============================================================
    // 创建支付 - 重复提交
    // ============================================================

    @Test
    @DisplayName("创建支付 - 幂等性（重复付款单号拦截）")
    void createPayment_DuplicatePaymentNo() {
        Long userId = 1L;
        PayRequest request = new PayRequest();
        request.setOrderId(1001L);
        request.setAmount(new BigDecimal("99.00"));
        request.setPayChannel(1);
        request.setPayMethod("WECHAT");

        when(valueOperations.setIfAbsent(anyString(), eq("1"), eq(30L), eq(TimeUnit.SECONDS))).thenReturn(true);
        when(paymentRecordMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        assertThrows(BusinessException.class, () -> paymentService.createPayment(userId, request));
        verify(paymentRecordMapper, never()).insert(any(PaymentRecord.class));
    }

    // ============================================================
    // 辅助方法
    // ============================================================

    private PaymentRecord buildPaymentRecord(Long id, String paymentNo, BigDecimal amount, int payStatus) {
        PaymentRecord record = new PaymentRecord();
        record.setId(id);
        record.setPaymentNo(paymentNo);
        record.setOrderId(1001L);
        record.setUserId(1L);
        record.setAmount(amount);
        record.setPayStatus(payStatus);
        record.setPayMethod("WECHAT");
        record.setPayChannel(1);
        return record;
    }
}