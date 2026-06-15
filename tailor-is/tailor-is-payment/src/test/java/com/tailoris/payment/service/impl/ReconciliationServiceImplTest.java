package com.tailoris.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tailoris.payment.entity.PaymentRecord;
import com.tailoris.payment.entity.ReconciliationRecord;
import com.tailoris.payment.mapper.PaymentRecordMapper;
import com.tailoris.payment.mapper.ReconciliationRecordMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("ReconciliationServiceImpl 单元测试")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReconciliationServiceImplTest {

    @Mock
    private ReconciliationRecordMapper reconciliationRecordMapper;

    @Mock
    private PaymentRecordMapper paymentRecordMapper;

    @InjectMocks
    private ReconciliationServiceImpl reconciliationService;

    private ReconciliationRecord reconciliationRecord;
    private PaymentRecord paymentRecord;

    @BeforeEach
    void setUp() {
        reconciliationRecord = new ReconciliationRecord();
        reconciliationRecord.setId(1L);
        reconciliationRecord.setOrderId(100L);
        reconciliationRecord.setAmount(new BigDecimal("1000.00"));
        reconciliationRecord.setChannel("wechat");
        reconciliationRecord.setChannelTradeNo("TXN123456");
        reconciliationRecord.setStatus(0);
        reconciliationRecord.setDiffAmount(BigDecimal.ZERO);

        paymentRecord = new PaymentRecord();
        paymentRecord.setId(1L);
        paymentRecord.setOrderId(100L);
        paymentRecord.setAmount(new BigDecimal("1000.00"));
        paymentRecord.setPayStatus(1);
    }

    @Test
    @DisplayName("创建对账记录-成功")
    void testRecordPayment_Success() {
        when(reconciliationRecordMapper.insert(any(ReconciliationRecord.class))).thenReturn(1);

        assertDoesNotThrow(() -> {
            reconciliationService.recordPayment(
                    100L,
                    new BigDecimal("1000.00"),
                    "wechat",
                    "TXN123456"
            );
        });

        verify(reconciliationRecordMapper).insert(any(ReconciliationRecord.class));
    }

    @Test
    @DisplayName("对账-记录不存在")
    void testReconcile_RecordNotFound() {
        when(reconciliationRecordMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        assertDoesNotThrow(() -> reconciliationService.reconcile(999L));

        verify(reconciliationRecordMapper, never()).updateById((ReconciliationRecord) any());
    }

    @Test
    @DisplayName("对账-金额一致")
    void testReconcile_AmountMatch() {
        when(reconciliationRecordMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(reconciliationRecord);
        when(paymentRecordMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.singletonList(paymentRecord));
        when(reconciliationRecordMapper.updateById(any(ReconciliationRecord.class))).thenReturn(1);

        reconciliationService.reconcile(100L);

        assertEquals(1, reconciliationRecord.getStatus());
        assertEquals(0, reconciliationRecord.getDiffAmount().compareTo(BigDecimal.ZERO));
        assertNotNull(reconciliationRecord.getReconciledAt());
        verify(reconciliationRecordMapper).updateById(any(ReconciliationRecord.class));
    }

    @Test
    @DisplayName("对账-金额不一致（少付）")
    void testReconcile_AmountMismatch_Less() {
        PaymentRecord partialPayment = new PaymentRecord();
        partialPayment.setId(1L);
        partialPayment.setOrderId(100L);
        partialPayment.setAmount(new BigDecimal("800.00"));
        partialPayment.setPayStatus(1);

        when(reconciliationRecordMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(reconciliationRecord);
        when(paymentRecordMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.singletonList(partialPayment));
        when(reconciliationRecordMapper.updateById(any(ReconciliationRecord.class))).thenReturn(1);

        reconciliationService.reconcile(100L);

        assertEquals(2, reconciliationRecord.getStatus());
        assertEquals(0, reconciliationRecord.getDiffAmount().compareTo(new BigDecimal("200.00")));
        assertNotNull(reconciliationRecord.getReconciledAt());
    }

    @Test
    @DisplayName("对账-金额不一致（多付）")
    void testReconcile_AmountMismatch_More() {
        PaymentRecord overPayment = new PaymentRecord();
        overPayment.setId(1L);
        overPayment.setOrderId(100L);
        overPayment.setAmount(new BigDecimal("1200.00"));
        overPayment.setPayStatus(1);

        when(reconciliationRecordMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(reconciliationRecord);
        when(paymentRecordMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.singletonList(overPayment));
        when(reconciliationRecordMapper.updateById(any(ReconciliationRecord.class))).thenReturn(1);

        reconciliationService.reconcile(100L);

        assertEquals(2, reconciliationRecord.getStatus());
        assertEquals(0, reconciliationRecord.getDiffAmount().compareTo(new BigDecimal("-200.00")));
    }

    @Test
    @DisplayName("对账-无支付记录")
    void testReconcile_NoPayments() {
        when(reconciliationRecordMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(reconciliationRecord);
        when(paymentRecordMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());
        when(reconciliationRecordMapper.updateById(any(ReconciliationRecord.class))).thenReturn(1);

        reconciliationService.reconcile(100L);

        assertEquals(2, reconciliationRecord.getStatus());
        assertEquals(0, reconciliationRecord.getDiffAmount().compareTo(new BigDecimal("1000.00")));
    }

    @Test
    @DisplayName("对账-过滤未支付记录")
    void testReconcile_FilterUnpaidRecords() {
        PaymentRecord unpaidRecord = new PaymentRecord();
        unpaidRecord.setId(2L);
        unpaidRecord.setOrderId(100L);
        unpaidRecord.setAmount(new BigDecimal("1000.00"));
        unpaidRecord.setPayStatus(0);

        when(reconciliationRecordMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(reconciliationRecord);
        when(paymentRecordMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.singletonList(unpaidRecord));
        when(reconciliationRecordMapper.updateById(any(ReconciliationRecord.class))).thenReturn(1);

        reconciliationService.reconcile(100L);

        assertEquals(2, reconciliationRecord.getStatus());
        assertEquals(0, reconciliationRecord.getDiffAmount().compareTo(new BigDecimal("1000.00")));
    }

    @Test
    @DisplayName("对账-处理null金额")
    void testReconcile_NullAmount() {
        PaymentRecord nullAmountRecord = new PaymentRecord();
        nullAmountRecord.setId(1L);
        nullAmountRecord.setOrderId(100L);
        nullAmountRecord.setAmount(null);
        nullAmountRecord.setPayStatus(1);

        when(reconciliationRecordMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(reconciliationRecord);
        when(paymentRecordMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.singletonList(nullAmountRecord));
        when(reconciliationRecordMapper.updateById(any(ReconciliationRecord.class))).thenReturn(1);

        reconciliationService.reconcile(100L);

        assertEquals(2, reconciliationRecord.getStatus());
        assertEquals(0, reconciliationRecord.getDiffAmount().compareTo(new BigDecimal("1000.00")));
    }

    @Test
    @DisplayName("查询未对账记录列表")
    void testListUnreconciled() {
        when(reconciliationRecordMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.singletonList(reconciliationRecord));

        List<ReconciliationRecord> result = reconciliationService.listUnreconciled();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(reconciliationRecord, result.get(0));
    }

    @Test
    @DisplayName("查询未对账记录-空列表")
    void testListUnreconciled_Empty() {
        when(reconciliationRecordMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.emptyList());

        List<ReconciliationRecord> result = reconciliationService.listUnreconciled();

        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    @DisplayName("批量对账-成功")
    void testBatchReconcile_Success() {
        when(reconciliationRecordMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(reconciliationRecord);
        when(paymentRecordMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.singletonList(paymentRecord));
        when(reconciliationRecordMapper.updateById(any(ReconciliationRecord.class))).thenReturn(1);

        List<Long> orderIds = Arrays.asList(100L, 101L, 102L);

        assertDoesNotThrow(() -> reconciliationService.batchReconcile(orderIds));

        verify(reconciliationRecordMapper, times(3)).selectOne(any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("批量对账-部分失败继续执行")
    void testBatchReconcile_PartialFailure() {
        ReconciliationRecord record1 = new ReconciliationRecord();
        record1.setId(1L);
        record1.setOrderId(100L);
        record1.setAmount(new BigDecimal("1000.00"));
        record1.setStatus(0);
        record1.setDiffAmount(BigDecimal.ZERO);

        when(reconciliationRecordMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(record1)
                .thenThrow(new RuntimeException("数据库错误"));
        when(paymentRecordMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.singletonList(paymentRecord));
        when(reconciliationRecordMapper.updateById(any(ReconciliationRecord.class))).thenReturn(1);

        List<Long> orderIds = Arrays.asList(100L, 101L, 102L);

        assertDoesNotThrow(() -> reconciliationService.batchReconcile(orderIds));

        verify(reconciliationRecordMapper, times(3)).selectOne(any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("批量对账-空列表")
    void testBatchReconcile_EmptyList() {
        assertDoesNotThrow(() -> reconciliationService.batchReconcile(Collections.emptyList()));

        verify(reconciliationRecordMapper, never()).selectOne(any());
    }
}
