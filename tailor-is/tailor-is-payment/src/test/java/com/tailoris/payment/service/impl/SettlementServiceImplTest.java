package com.tailoris.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tailoris.common.exception.BusinessException;
import com.tailoris.payment.dto.SettlementQueryRequest;
import com.tailoris.payment.entity.MerchantAccount;
import com.tailoris.payment.entity.SettlementRecord;
import com.tailoris.payment.mapper.MerchantAccountMapper;
import com.tailoris.payment.mapper.SettlementRecordMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("SettlementServiceImpl 单元测试")
@ExtendWith(MockitoExtension.class)
class SettlementServiceImplTest {

    @Mock
    private SettlementRecordMapper settlementRecordMapper;

    @Mock
    private MerchantAccountMapper merchantAccountMapper;

    @InjectMocks
    private SettlementServiceImpl settlementService;

    private static final Long MERCHANT_ID = 100L;
    private static final Long SHOP_ID = 200L;
    private static final Long ORDER_ID = 1001L;

    @Test
    @DisplayName("单笔订单结算成功")
    void testSettleOrder_Success() {
        BigDecimal orderAmount = new BigDecimal("100.00");
        BigDecimal feeRate = new BigDecimal("0.05");

        when(settlementRecordMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(settlementRecordMapper.insert(any(SettlementRecord.class))).thenReturn(1);

        MerchantAccount account = new MerchantAccount();
        account.setMerchantId(MERCHANT_ID);
        account.setPendingAmount(new BigDecimal("500.00"));
        when(merchantAccountMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(account);
        when(merchantAccountMapper.updateById(any(MerchantAccount.class))).thenReturn(1);

        SettlementRecord result = settlementService.settleOrder(ORDER_ID, MERCHANT_ID, SHOP_ID, orderAmount, feeRate);

        assertNotNull(result);
        assertEquals(ORDER_ID, result.getOrderId());
        assertEquals(new BigDecimal("5.00"), result.getPlatformFee());
        assertEquals(new BigDecimal("95.00"), result.getMerchantAmount());
        assertEquals(0, result.getStatus());
    }

    @Test
    @DisplayName("重复结算订单抛出异常")
    void testSettleOrder_Duplicate() {
        when(settlementRecordMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        assertThrows(BusinessException.class, () ->
                settlementService.settleOrder(ORDER_ID, MERCHANT_ID, SHOP_ID, new BigDecimal("100.00"), new BigDecimal("0.05")));
    }

    @Test
    @DisplayName("批量结算多笔订单")
    void testBatchSettle_Success() {
        List<Long> orderIds = Arrays.asList(1001L, 1002L);

        SettlementRecord record1 = new SettlementRecord();
        record1.setId(1L);
        record1.setOrderId(1001L);
        record1.setStatus(0);
        record1.setMerchantAmount(new BigDecimal("95.00"));

        SettlementRecord record2 = new SettlementRecord();
        record2.setId(2L);
        record2.setOrderId(1002L);
        record2.setStatus(0);
        record2.setMerchantAmount(new BigDecimal("190.00"));

        when(settlementRecordMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Arrays.asList(record1, record2));
        when(settlementRecordMapper.updateById(any(SettlementRecord.class))).thenReturn(1);

        MerchantAccount account = new MerchantAccount();
        account.setMerchantId(MERCHANT_ID);
        account.setPendingAmount(new BigDecimal("285.00"));
        account.setWithdrawableBalance(new BigDecimal("100.00"));
        account.setTotalSettlement(new BigDecimal("500.00"));
        when(merchantAccountMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(account);
        when(merchantAccountMapper.updateById(any(MerchantAccount.class))).thenReturn(1);

        settlementService.batchSettle(MERCHANT_ID, orderIds);

        verify(settlementRecordMapper, times(2)).updateById(any(SettlementRecord.class));
        verify(merchantAccountMapper).updateById((MerchantAccount) argThat(a -> {
                MerchantAccount ma = (MerchantAccount) a;
                return ma.getWithdrawableBalance().compareTo(new BigDecimal("385.00")) == 0;
        }));
    }

    @Test
    @DisplayName("空订单列表批量结算直接返回")
    void testBatchSettle_EmptyList() {
        settlementService.batchSettle(MERCHANT_ID, Collections.emptyList());

        verify(settlementRecordMapper, never()).selectList(any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("按编号查询结算记录-不存在返回null")
    void testGetSettlementByNo_NotFound() {
        when(settlementRecordMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        SettlementRecord result = settlementService.getSettlementByNo("SETTLE_NOT_EXIST");

        assertNull(result);
    }

    @Test
    @DisplayName("结算金额计算正确-高费率")
    void testSettleOrder_HighFeeRate() {
        BigDecimal orderAmount = new BigDecimal("10000.00");
        BigDecimal feeRate = new BigDecimal("0.15");

        when(settlementRecordMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(settlementRecordMapper.insert(any(SettlementRecord.class))).thenReturn(1);

        MerchantAccount account = new MerchantAccount();
        account.setMerchantId(MERCHANT_ID);
        account.setPendingAmount(new BigDecimal("0.00"));
        when(merchantAccountMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(account);
        when(merchantAccountMapper.updateById(any(MerchantAccount.class))).thenReturn(1);

        SettlementRecord result = settlementService.settleOrder(ORDER_ID, MERCHANT_ID, SHOP_ID, orderAmount, feeRate);

        assertNotNull(result);
        assertEquals(new BigDecimal("1500.00"), result.getPlatformFee());
        assertEquals(new BigDecimal("8500.00"), result.getMerchantAmount());
    }

    @Test
    @DisplayName("查询结算列表-带条件过滤")
    void testGetMerchantSettlement_WithFilters() {
        SettlementQueryRequest request = new SettlementQueryRequest();
        request.setPageNum(1);
        request.setPageSize(5);
        request.setStatus(0);
        request.setSettlementType(1);

        when(settlementRecordMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                .thenReturn(new Page<>(1, 5));

        com.tailoris.common.dto.PageResponse<SettlementRecord> result =
                settlementService.getMerchantSettlement(MERCHANT_ID, request);

        assertNotNull(result);
        assertEquals(0L, result.getTotal());
    }
}