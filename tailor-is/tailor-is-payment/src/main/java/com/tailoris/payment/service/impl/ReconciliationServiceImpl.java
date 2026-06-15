package com.tailoris.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tailoris.common.util.SnowflakeIdGenerator;
import com.tailoris.payment.entity.PaymentRecord;
import com.tailoris.payment.entity.ReconciliationRecord;
import com.tailoris.payment.mapper.PaymentRecordMapper;
import com.tailoris.payment.mapper.ReconciliationRecordMapper;
import com.tailoris.payment.service.ReconciliationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReconciliationServiceImpl implements ReconciliationService {

    private final ReconciliationRecordMapper reconciliationRecordMapper;
    private final PaymentRecordMapper paymentRecordMapper;

    @Override
    @Transactional
    public void recordPayment(Long orderId, BigDecimal amount, String channel, String channelTradeNo) {
        ReconciliationRecord record = new ReconciliationRecord();
        record.setId(SnowflakeIdGenerator.getInstance().nextId());
        record.setOrderId(orderId);
        record.setAmount(amount);
        record.setChannel(channel);
        record.setChannelTradeNo(channelTradeNo);
        record.setStatus(0);
        record.setDiffAmount(BigDecimal.ZERO);
        reconciliationRecordMapper.insert(record);
        log.info("对账记录已创建: orderId={}, amount={}, channel={}, tradeNo={}", orderId, amount, channel, channelTradeNo);
    }

    @Override
    @Transactional
    public void reconcile(Long orderId) {
        LambdaQueryWrapper<ReconciliationRecord> query = new LambdaQueryWrapper<>();
        query.eq(ReconciliationRecord::getOrderId, orderId);
        ReconciliationRecord record = reconciliationRecordMapper.selectOne(query);

        if (record == null) {
            log.warn("对账记录不存在: orderId={}", orderId);
            return;
        }

        LambdaQueryWrapper<PaymentRecord> paymentQuery = new LambdaQueryWrapper<>();
        paymentQuery.eq(PaymentRecord::getOrderId, orderId);
        List<PaymentRecord> payments = paymentRecordMapper.selectList(paymentQuery);

        BigDecimal paidAmount = payments.stream()
                .filter(p -> p.getPayStatus() != null && p.getPayStatus() == 1)
                .map(p -> p.getAmount() != null ? p.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal diff = record.getAmount().subtract(paidAmount);

        if (diff.compareTo(BigDecimal.ZERO) == 0) {
            record.setStatus(1);
            record.setDiffAmount(BigDecimal.ZERO);
            log.info("对账成功: orderId={}, 金额一致={}", orderId, record.getAmount());
        } else {
            record.setStatus(2);
            record.setDiffAmount(diff);
            log.warn("对账差异: orderId={}, 记录金额={}, 实付金额={}, 差额={}", orderId, record.getAmount(), paidAmount, diff);
        }

        record.setReconciledAt(LocalDateTime.now());
        reconciliationRecordMapper.updateById(record);
    }

    @Override
    public List<ReconciliationRecord> listUnreconciled() {
        LambdaQueryWrapper<ReconciliationRecord> query = new LambdaQueryWrapper<>();
        query.eq(ReconciliationRecord::getStatus, 0);
        return reconciliationRecordMapper.selectList(query);
    }

    @Override
    @Transactional
    public void batchReconcile(List<Long> orderIds) {
        for (Long orderId : orderIds) {
            try {
                reconcile(orderId);
            } catch (Exception e) {
                log.error("批量对账失败: orderId={}", orderId, e);
            }
        }
    }
}