package com.tailoris.payment.service;

import com.tailoris.common.dto.PageRequest;
import com.tailoris.common.dto.PageResponse;
import com.tailoris.payment.dto.SettlementQueryRequest;
import com.tailoris.payment.entity.SettlementRecord;

import java.math.BigDecimal;
import java.util.List;

public interface SettlementService {

    SettlementRecord settleOrder(Long orderId, Long merchantId, Long shopId, BigDecimal orderAmount, BigDecimal platformFeeRate);

    void batchSettle(Long merchantId, List<Long> orderIds);

    PageResponse<SettlementRecord> getMerchantSettlement(Long merchantId, SettlementQueryRequest request);

    SettlementRecord getSettlementByNo(String settlementNo);
}
