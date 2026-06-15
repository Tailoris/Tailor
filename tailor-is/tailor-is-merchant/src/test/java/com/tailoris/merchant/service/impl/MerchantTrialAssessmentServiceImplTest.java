package com.tailoris.merchant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tailoris.merchant.constant.MerchantConstants;
import com.tailoris.merchant.constant.TrialAssessmentConstants;
import com.tailoris.merchant.entity.Merchant;
import com.tailoris.merchant.entity.MerchantTrialAssessment;
import com.tailoris.merchant.exception.MerchantBusinessException;
import com.tailoris.merchant.mapper.MerchantMapper;
import com.tailoris.merchant.mapper.MerchantTrialAssessmentMapper;
import com.tailoris.merchant.service.IMerchantViolationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("商家试运营考核服务单元测试")
@ExtendWith(MockitoExtension.class)
class MerchantTrialAssessmentServiceImplTest {

    @Mock
    private MerchantMapper merchantMapper;

    @Mock
    private MerchantTrialAssessmentMapper trialMapper;

    @Mock
    private ObjectProvider<IMerchantViolationService> violationServiceProvider;

    private MerchantTrialAssessmentServiceImpl trialService;

    private Merchant merchant;
    private MerchantTrialAssessment record;

    @BeforeEach
    void setUp() {
        trialService = new MerchantTrialAssessmentServiceImpl(merchantMapper, violationServiceProvider);
        ReflectionTestUtils.setField(trialService, "baseMapper", trialMapper);

        merchant = new Merchant();
        merchant.setId(1L);
        merchant.setUserId(100L);
        merchant.setStatus(MerchantConstants.MERCHANT_STATUS_NORMAL);
        merchant.setIsTrial(1);
        merchant.setIsPromoted(0);

        record = new MerchantTrialAssessment();
        record.setId(1L);
        record.setMerchantId(1L);
        record.setTrialStartDate(LocalDate.now().minusDays(15).toString());
        record.setTrialEndDate(LocalDate.now().plusDays(15).toString());
        record.setTotalDays(TrialAssessmentConstants.TRIAL_DAYS);
        record.setActualDays(15);
        record.setOrderCount(15L);
        record.setOrderAmount(new BigDecimal("2000.00"));
        record.setProductCount(8L);
        record.setRefundRate(new BigDecimal("0.10"));
        record.setComplaintCount(2L);
        record.setViolationCount(0L);
        record.setScore(new BigDecimal("85.00"));
        record.setResult(TrialAssessmentConstants.RESULT_PENDING);
        record.setIsPromoted(0);
    }

    @Test
    @DisplayName("创建试运营记录：商家不存在应抛异常")
    void testCreateTrialRecord_MerchantNotFound() {
        when(merchantMapper.selectById(999L)).thenReturn(null);

        assertThrows(MerchantBusinessException.class, () -> trialService.createTrialRecord(999L));
    }

    @Test
    @DisplayName("创建试运营记录：已存在则返回现有记录")
    void testCreateTrialRecord_AlreadyExists() {
        when(merchantMapper.selectById(1L)).thenReturn(merchant);
        when(trialMapper.selectOne(any(LambdaQueryWrapper.class), anyBoolean())).thenReturn(record);

        MerchantTrialAssessment result = trialService.createTrialRecord(1L);

        assertNotNull(result);
        assertEquals(record.getId(), result.getId());
    }

    @Test
    @DisplayName("创建试运营记录：成功创建新记录")
    void testCreateTrialRecord_Success() {
        when(merchantMapper.selectById(1L)).thenReturn(merchant);
        when(trialMapper.selectOne(any(LambdaQueryWrapper.class), anyBoolean())).thenReturn(null);
        when(trialMapper.insert(any(MerchantTrialAssessment.class))).thenReturn(1);
        when(merchantMapper.updateById(any(Merchant.class))).thenReturn(1);

        MerchantTrialAssessment result = trialService.createTrialRecord(1L);

        assertNotNull(result);
        assertEquals(1L, result.getMerchantId());
        assertEquals(TrialAssessmentConstants.RESULT_PENDING, result.getResult());
        assertEquals(1, merchant.getIsTrial());
    }

    @Test
    @DisplayName("执行考核：未找到记录应抛异常")
    void testPerformAssessment_RecordNotFound() {
        when(trialMapper.selectOne(any(LambdaQueryWrapper.class), anyBoolean())).thenReturn(null);

        assertThrows(MerchantBusinessException.class, () -> trialService.performAssessment(1L));
    }

    @Test
    @DisplayName("执行考核：成功通过考核")
    void testPerformAssessment_Pass() {
        record.setOrderCount(15L);
        record.setOrderAmount(new BigDecimal("2000.00"));
        record.setProductCount(8L);
        record.setRefundRate(new BigDecimal("0.10"));
        record.setComplaintCount(1L);
        record.setViolationCount(0L);

        when(trialMapper.selectOne(any(LambdaQueryWrapper.class), anyBoolean())).thenReturn(record);
        when(trialMapper.updateById(any(MerchantTrialAssessment.class))).thenReturn(1);

        MerchantTrialAssessment result = trialService.performAssessment(1L);

        assertNotNull(result);
        assertNotNull(result.getScore());
        assertEquals(TrialAssessmentConstants.RESULT_PASS, result.getResult());
    }

    @Test
    @DisplayName("执行考核：违规次数过多未通过")
    void testPerformAssessment_FailByViolationCount() {
        record.setViolationCount(5L);

        when(trialMapper.selectOne(any(LambdaQueryWrapper.class), anyBoolean())).thenReturn(record);
        when(trialMapper.updateById(any(MerchantTrialAssessment.class))).thenReturn(1);

        MerchantTrialAssessment result = trialService.performAssessment(1L);

        assertNotNull(result);
        assertEquals(TrialAssessmentConstants.RESULT_FAIL, result.getResult());
    }

    @Test
    @DisplayName("执行考核：投诉数过多未通过")
    void testPerformAssessment_FailByComplaintCount() {
        record.setComplaintCount(10L);

        when(trialMapper.selectOne(any(LambdaQueryWrapper.class), anyBoolean())).thenReturn(record);
        when(trialMapper.updateById(any(MerchantTrialAssessment.class))).thenReturn(1);

        MerchantTrialAssessment result = trialService.performAssessment(1L);

        assertNotNull(result);
        assertEquals(TrialAssessmentConstants.RESULT_FAIL, result.getResult());
    }

    @Test
    @DisplayName("执行考核：退款率过高未通过")
    void testPerformAssessment_FailByRefundRate() {
        record.setRefundRate(new BigDecimal("0.60"));

        when(trialMapper.selectOne(any(LambdaQueryWrapper.class), anyBoolean())).thenReturn(record);
        when(trialMapper.updateById(any(MerchantTrialAssessment.class))).thenReturn(1);

        MerchantTrialAssessment result = trialService.performAssessment(1L);

        assertNotNull(result);
        assertEquals(TrialAssessmentConstants.RESULT_FAIL, result.getResult());
    }

    @Test
    @DisplayName("执行考核：分数低于60未通过")
    void testPerformAssessment_FailByLowScore() {
        record.setOrderCount(0L);
        record.setOrderAmount(BigDecimal.ZERO);
        record.setProductCount(0L);
        record.setRefundRate(new BigDecimal("0.30"));
        record.setComplaintCount(4L);
        record.setViolationCount(2L);

        when(trialMapper.selectOne(any(LambdaQueryWrapper.class), anyBoolean())).thenReturn(record);
        when(trialMapper.updateById(any(MerchantTrialAssessment.class))).thenReturn(1);

        MerchantTrialAssessment result = trialService.performAssessment(1L);

        assertNotNull(result);
        assertEquals(TrialAssessmentConstants.RESULT_FAIL, result.getResult());
    }

    @Test
    @DisplayName("执行考核：分数60-80延期")
    void testPerformAssessment_Extend() {
        // Reset record to clean state
        // orderCount=6 → score=18, orderAmount=600 → score=18, productCount=4 → score=16
        // refundRate=0.15 → score=10, violationCount=1 → score=7, total=69
        record.setOrderCount(6L);
        record.setOrderAmount(new BigDecimal("600.00"));
        record.setProductCount(4L);
        record.setRefundRate(new BigDecimal("0.15"));
        record.setComplaintCount(2L);
        record.setViolationCount(1L);
        record.setResult(TrialAssessmentConstants.RESULT_PENDING);

        when(trialMapper.selectOne(any(LambdaQueryWrapper.class), anyBoolean())).thenReturn(record);
        when(trialMapper.updateById(any(MerchantTrialAssessment.class))).thenReturn(1);

        MerchantTrialAssessment result = trialService.performAssessment(1L);

        assertNotNull(result);
        assertEquals(TrialAssessmentConstants.RESULT_EXTEND, result.getResult());
    }

    @Test
    @DisplayName("商家转正：未找到记录应抛异常")
    void testPromote_RecordNotFound() {
        when(trialMapper.selectOne(any(LambdaQueryWrapper.class), anyBoolean())).thenReturn(null);

        assertThrows(MerchantBusinessException.class, () -> trialService.promote(1L, "转正"));
    }

    @Test
    @DisplayName("商家转正：考核未通过应抛异常")
    void testPromote_NotPassed() {
        record.setResult(TrialAssessmentConstants.RESULT_FAIL);
        when(trialMapper.selectOne(any(LambdaQueryWrapper.class), anyBoolean())).thenReturn(record);

        assertThrows(MerchantBusinessException.class, () -> trialService.promote(1L, "转正"));
    }

    @Test
    @DisplayName("商家转正：成功转正")
    void testPromote_Success() {
        record.setResult(TrialAssessmentConstants.RESULT_PASS);
        when(trialMapper.selectOne(any(LambdaQueryWrapper.class), anyBoolean())).thenReturn(record);
        when(trialMapper.updateById(any(MerchantTrialAssessment.class))).thenReturn(1);
        when(merchantMapper.selectById(1L)).thenReturn(merchant);
        when(merchantMapper.updateById(any(Merchant.class))).thenReturn(1);

        boolean result = trialService.promote(1L, "考核通过，准予转正");

        assertTrue(result);
        assertEquals(1, record.getIsPromoted());
        assertEquals(0, merchant.getIsTrial());
        assertEquals(1, merchant.getIsPromoted());
    }

    @Test
    @DisplayName("延期考核：未找到记录应抛异常")
    void testExtend_RecordNotFound() {
        when(trialMapper.selectOne(any(LambdaQueryWrapper.class), anyBoolean())).thenReturn(null);

        assertThrows(MerchantBusinessException.class, () -> trialService.extend(1L, 30, "延期"));
    }

    @Test
    @DisplayName("延期考核：成功延期")
    void testExtend_Success() {
        when(trialMapper.selectOne(any(LambdaQueryWrapper.class), anyBoolean())).thenReturn(record);
        when(trialMapper.updateById(any(MerchantTrialAssessment.class))).thenReturn(1);
        when(merchantMapper.selectById(1L)).thenReturn(merchant);
        when(merchantMapper.updateById(any(Merchant.class))).thenReturn(1);

        boolean result = trialService.extend(1L, 30, "表现一般，再观察30天");

        assertTrue(result);
        assertEquals(TrialAssessmentConstants.RESULT_EXTEND, record.getResult());
        assertEquals(TrialAssessmentConstants.TRIAL_DAYS + 30, record.getTotalDays());
    }

    @Test
    @DisplayName("考核不通过关闭店铺：未找到记录应抛异常")
    void testFailAndClose_RecordNotFound() {
        when(trialMapper.selectOne(any(LambdaQueryWrapper.class), anyBoolean())).thenReturn(null);

        assertThrows(MerchantBusinessException.class, () -> trialService.failAndClose(1L, "不通过"));
    }

    @Test
    @DisplayName("考核不通过关闭店铺：成功关闭")
    void testFailAndClose_Success() {
        when(trialMapper.selectOne(any(LambdaQueryWrapper.class), anyBoolean())).thenReturn(record);
        when(trialMapper.updateById(any(MerchantTrialAssessment.class))).thenReturn(1);
        when(merchantMapper.selectById(1L)).thenReturn(merchant);
        when(merchantMapper.updateById(any(Merchant.class))).thenReturn(1);

        boolean result = trialService.failAndClose(1L, "考核不通过");

        assertTrue(result);
        assertEquals(TrialAssessmentConstants.RESULT_FAIL, record.getResult());
        assertEquals(MerchantConstants.MERCHANT_STATUS_CANCELLED, merchant.getStatus());
    }

    @Test
    @DisplayName("查询待考核商家列表：成功返回")
    void testListPendingAssessments_Success() {
        List<MerchantTrialAssessment> list = Arrays.asList(record);
        when(trialMapper.selectPendingAssessment()).thenReturn(list);

        List<MerchantTrialAssessment> result = trialService.listPendingAssessments();

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("查询考核历史：成功返回")
    void testGetAssessmentHistory_Success() {
        List<MerchantTrialAssessment> list = Arrays.asList(record);
        when(trialMapper.selectByMerchantId(1L)).thenReturn(list);

        List<MerchantTrialAssessment> result = trialService.getAssessmentHistory(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
    }
}
