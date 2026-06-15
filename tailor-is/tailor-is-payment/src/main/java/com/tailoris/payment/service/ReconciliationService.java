package com.tailoris.payment.service;

import com.tailoris.payment.entity.ReconciliationRecord;

import java.math.BigDecimal;
import java.util.List;

public interface ReconciliationService {

    void recordPayment(Long orderId, BigDecimal amount, String channel, String channelTradeNo);

    void reconcile(Long orderId);

    List<ReconciliationRecord> listUnreconciled();

    void batchReconcile(List<Long> orderIds);
}