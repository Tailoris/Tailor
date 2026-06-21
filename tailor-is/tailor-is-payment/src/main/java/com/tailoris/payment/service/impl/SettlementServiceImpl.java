package com.tailoris.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tailoris.common.dto.PageRequest;
import com.tailoris.common.dto.PageResponse;
import com.tailoris.common.exception.BusinessException;
import com.tailoris.common.util.SnowflakeIdGenerator;
import com.tailoris.payment.dto.SettlementQueryRequest;
import com.tailoris.payment.entity.MerchantAccount;
import com.tailoris.payment.entity.SettlementRecord;
import com.tailoris.payment.mapper.MerchantAccountMapper;
import com.tailoris.payment.mapper.SettlementRecordMapper;
import com.tailoris.payment.service.SettlementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.util.CollectionUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class SettlementServiceImpl implements SettlementService {

    private final SettlementRecordMapper settlementRecordMapper;
    private final MerchantAccountMapper merchantAccountMapper;

    @Override
    @Transactional
    public SettlementRecord settleOrder(Long orderId, Long merchantId, Long shopId, BigDecimal orderAmount, BigDecimal platformFeeRate) {
        LambdaQueryWrapper<SettlementRecord> existQuery = new LambdaQueryWrapper<>();
        existQuery.eq(SettlementRecord::getOrderId, orderId);
        Long existCount = settlementRecordMapper.selectCount(existQuery);
        if (existCount > 0) {
            throw new BusinessException("该订单已结算");
        }

        BigDecimal platformFee = orderAmount.multiply(platformFeeRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal merchantAmount = orderAmount.subtract(platformFee);

        SettlementRecord record = new SettlementRecord();
        record.setId(SnowflakeIdGenerator.getInstance().nextId());
        record.setSettlementNo("SETTLE" + SnowflakeIdGenerator.getInstance().nextId());
        record.setMerchantId(merchantId);
        record.setShopId(shopId);
        record.setOrderId(orderId);
        record.setOrderNo("ORD" + orderId);
        record.setSettlementType(1);
        record.setOrderAmount(orderAmount);
        record.setPlatformFee(platformFee);
        record.setPlatformFeeRate(platformFeeRate);
        record.setCouponSubsidy(BigDecimal.ZERO);
        record.setMerchantAmount(merchantAmount);
        record.setStatus(0);
        record.setSettlementCycle(1);
        settlementRecordMapper.insert(record);

        updateMerchantPendingAmount(merchantId, merchantAmount);

        return record;
    }

    @Override
    @Transactional
    public void batchSettle(Long merchantId, List<Long> orderIds) {
        if (CollectionUtils.isEmpty(orderIds)) {
            return;
        }

        LambdaQueryWrapper<SettlementRecord> batchQuery = new LambdaQueryWrapper<>();
        batchQuery.in(SettlementRecord::getOrderId, orderIds);
        List<SettlementRecord> records = settlementRecordMapper.selectList(batchQuery);

        Map<Long, SettlementRecord> recordMap = records.stream()
                .collect(Collectors.toMap(SettlementRecord::getOrderId, Function.identity()));

        BigDecimal totalMerchantAmount = BigDecimal.ZERO;

        for (Long orderId : orderIds) {
            try {
                SettlementRecord record = recordMap.get(orderId);
                if (record != null && record.getStatus() == 0) {
                    record.setStatus(2);
                    record.setSettledAt(LocalDateTime.now());
                    settlementRecordMapper.updateById(record);
                    totalMerchantAmount = totalMerchantAmount.add(record.getMerchantAmount());
                }
            } catch (Exception e) {
                log.error("批量结算失败，orderId={}", orderId, e);
            }
        }

        if (totalMerchantAmount.compareTo(BigDecimal.ZERO) > 0) {
            updateMerchantWithdrawableBalance(merchantId, totalMerchantAmount);
        }
    }

    @Override
    public PageResponse<SettlementRecord> getMerchantSettlement(Long merchantId, SettlementQueryRequest request) {
        Page<SettlementRecord> page = new Page<>(request.getPageNum(), request.getPageSize());
        LambdaQueryWrapper<SettlementRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SettlementRecord::getMerchantId, merchantId);

        if (request.getStatus() != null) {
            wrapper.eq(SettlementRecord::getStatus, request.getStatus());
        }
        if (request.getSettlementType() != null) {
            wrapper.eq(SettlementRecord::getSettlementType, request.getSettlementType());
        }
        if (request.getStartTime() != null) {
            wrapper.ge(SettlementRecord::getCreateTime, request.getStartTime());
        }
        if (request.getEndTime() != null) {
            wrapper.le(SettlementRecord::getCreateTime, request.getEndTime());
        }
        wrapper.orderByDesc(SettlementRecord::getCreateTime);

        Page<SettlementRecord> result = settlementRecordMapper.selectPage(page, wrapper);
        return new PageResponse<>(
                result.getRecords(),
                result.getTotal(),
                request.getPageNum(),
                request.getPageSize()
        );
    }

    @Override
    public SettlementRecord getSettlementByNo(String settlementNo) {
        LambdaQueryWrapper<SettlementRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SettlementRecord::getSettlementNo, settlementNo);
        return settlementRecordMapper.selectOne(wrapper);
    }

    private void updateMerchantPendingAmount(Long merchantId, BigDecimal amount) {
        // BE-H-12: 使用原子 UPDATE 避免竞态条件
        merchantAccountMapper.addPendingAmountAtomic(merchantId, amount);
    }

    private void updateMerchantWithdrawableBalance(Long merchantId, BigDecimal amount) {
        // BE-H-12: 使用原子结算转账，避免竞态条件
        int rows = merchantAccountMapper.settleAtomic(merchantId, amount);
        if (rows == 0) {
            log.error("商家结算失败，待结算金额不足: merchantId={}, amount={}", merchantId, amount);
        }
    }
}
