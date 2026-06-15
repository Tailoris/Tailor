package com.tailoris.merchant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tailoris.merchant.constant.MerchantConstants;
import com.tailoris.merchant.constant.ViolationConstants;
import com.tailoris.merchant.dto.ViolationAppealRequest;
import com.tailoris.merchant.dto.ViolationPunishRequest;
import com.tailoris.merchant.dto.ViolationReportRequest;
import com.tailoris.merchant.entity.Merchant;
import com.tailoris.merchant.entity.MerchantViolation;
import com.tailoris.merchant.exception.MerchantBusinessException;
import com.tailoris.merchant.mapper.MerchantMapper;
import com.tailoris.merchant.mapper.MerchantViolationMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("商家违规处罚服务单元测试")
@ExtendWith(MockitoExtension.class)
class MerchantViolationServiceImplTest {

    @Mock
    private MerchantMapper merchantMapper;

    @Mock
    private MerchantViolationMapper violationMapper;

    private MerchantViolationServiceImpl violationService;

    private Merchant merchant;
    private MerchantViolation violation;

    @BeforeEach
    void setUp() {
        violationService = new MerchantViolationServiceImpl(merchantMapper);
        ReflectionTestUtils.setField(violationService, "baseMapper", violationMapper);

        merchant = new Merchant();
        merchant.setId(1L);
        merchant.setUserId(100L);
        merchant.setViolationScore(100);
        merchant.setStatus(MerchantConstants.MERCHANT_STATUS_NORMAL);

        violation = new MerchantViolation();
        violation.setId(1L);
        violation.setMerchantId(1L);
        violation.setShopId(1L);
        violation.setViolationType(ViolationConstants.TYPE_PRODUCT);
        violation.setStatus(ViolationConstants.STATUS_PENDING);
        violation.setPunishmentType(ViolationConstants.PUNISH_PENDING);
        violation.setIsAppealed(ViolationConstants.APPEAL_NO);
    }

    @Test
    @DisplayName("提交违规举报：商家不存在应抛异常")
    void testReport_MerchantNotFound() {
        ViolationReportRequest request = new ViolationReportRequest();
        request.setMerchantId(999L);
        request.setViolationType(ViolationConstants.TYPE_PRODUCT);
        request.setTitle("测试违规");
        request.setDescription("测试描述");

        when(merchantMapper.selectById(999L)).thenReturn(null);

        assertThrows(MerchantBusinessException.class, () -> violationService.report(request));
    }

    @Test
    @DisplayName("提交违规举报：成功创建")
    void testReport_Success() {
        ViolationReportRequest request = new ViolationReportRequest();
        request.setMerchantId(1L);
        request.setShopId(1L);
        request.setViolationType(ViolationConstants.TYPE_PRODUCT);
        request.setTitle("测试违规");
        request.setDescription("测试描述");
        request.setEvidence("[]");
        request.setReporterId(100L);

        when(merchantMapper.selectById(1L)).thenReturn(merchant);
        when(violationMapper.insert(any(MerchantViolation.class))).thenReturn(1);

        MerchantViolation result = violationService.report(request);

        assertNotNull(result);
        assertEquals(1L, result.getMerchantId());
        assertEquals(ViolationConstants.STATUS_PENDING, result.getStatus());
        assertEquals(ViolationConstants.PUNISH_PENDING, result.getPunishmentType());
    }

    @Test
    @DisplayName("执行处罚：违规记录不存在应抛异常")
    void testPunish_ViolationNotFound() {
        ViolationPunishRequest request = new ViolationPunishRequest();
        request.setViolationId(999L);
        request.setViolationLevel(ViolationConstants.LEVEL_MINOR);
        request.setPunishmentType(ViolationConstants.PUNISH_WARN);
        request.setPunishmentDays(7);

        when(violationMapper.selectById(999L)).thenReturn(null);

        assertThrows(MerchantBusinessException.class, () -> violationService.punish(request));
    }

    @Test
    @DisplayName("执行处罚：状态不可处罚应抛异常")
    void testPunish_InvalidStatus() {
        ViolationPunishRequest request = new ViolationPunishRequest();
        request.setViolationId(1L);
        request.setViolationLevel(ViolationConstants.LEVEL_MINOR);
        request.setPunishmentType(ViolationConstants.PUNISH_WARN);

        violation.setStatus(ViolationConstants.STATUS_PUNISHED);
        when(violationMapper.selectById(1L)).thenReturn(violation);

        assertThrows(MerchantBusinessException.class, () -> violationService.punish(request));
    }

    @Test
    @DisplayName("执行处罚：成功处罚并更新商家状态")
    void testPunish_Success() {
        ViolationPunishRequest request = new ViolationPunishRequest();
        request.setViolationId(1L);
        request.setViolationLevel(ViolationConstants.LEVEL_GENERAL);
        request.setPunishmentType(ViolationConstants.PUNISH_LIMIT);
        request.setPunishmentDays(7);
        request.setHandlerId(1L);

        when(violationMapper.selectById(1L)).thenReturn(violation);
        when(violationMapper.updateById(any(MerchantViolation.class))).thenReturn(1);
        when(merchantMapper.selectById(1L)).thenReturn(merchant);
        when(merchantMapper.updateById(any(Merchant.class))).thenReturn(1);

        MerchantViolation result = violationService.punish(request);

        assertNotNull(result);
        assertEquals(ViolationConstants.STATUS_PUNISHED, result.getStatus());
        assertEquals(ViolationConstants.LEVEL_GENERAL, result.getViolationLevel());
        assertEquals(ViolationConstants.PUNISH_LIMIT, result.getPunishmentType());
        assertNotNull(result.getPunishmentStart());
        assertNotNull(result.getPunishmentEnd());
    }

    @Test
    @DisplayName("执行处罚：永久处罚无结束时间")
    void testPunish_Permanent() {
        ViolationPunishRequest request = new ViolationPunishRequest();
        request.setViolationId(1L);
        request.setViolationLevel(ViolationConstants.LEVEL_VERY_SERIOUS);
        request.setPunishmentType(ViolationConstants.PUNISH_EVICT);
        request.setPunishmentDays(0);
        request.setHandlerId(1L);

        when(violationMapper.selectById(1L)).thenReturn(violation);
        when(violationMapper.updateById(any(MerchantViolation.class))).thenReturn(1);
        when(merchantMapper.selectById(1L)).thenReturn(merchant);
        when(merchantMapper.updateById(any(Merchant.class))).thenReturn(1);

        MerchantViolation result = violationService.punish(request);

        assertNotNull(result);
        assertEquals(ViolationConstants.STATUS_PUNISHED, result.getStatus());
        assertNotNull(result.getPunishmentStart());
        assertNull(result.getPunishmentEnd());
        assertEquals(MerchantConstants.MERCHANT_STATUS_FROZEN, merchant.getStatus());
    }

    @Test
    @DisplayName("执行处罚：申诉中状态也可处罚")
    void testPunish_AppealedStatus() {
        ViolationPunishRequest request = new ViolationPunishRequest();
        request.setViolationId(1L);
        request.setViolationLevel(ViolationConstants.LEVEL_MINOR);
        request.setPunishmentType(ViolationConstants.PUNISH_WARN);
        request.setPunishmentDays(0);
        request.setHandlerId(1L);

        violation.setStatus(ViolationConstants.STATUS_APPEALED);
        when(violationMapper.selectById(1L)).thenReturn(violation);
        when(violationMapper.updateById(any(MerchantViolation.class))).thenReturn(1);
        when(merchantMapper.selectById(1L)).thenReturn(merchant);
        when(merchantMapper.updateById(any(Merchant.class))).thenReturn(1);

        MerchantViolation result = violationService.punish(request);
        assertNotNull(result);
        assertEquals(ViolationConstants.STATUS_PUNISHED, result.getStatus());
    }

    @Test
    @DisplayName("商家申诉：违规记录不存在应抛异常")
    void testAppeal_ViolationNotFound() {
        ViolationAppealRequest request = new ViolationAppealRequest();
        request.setViolationId(999L);
        request.setMerchantId(1L);
        request.setAppealContent("申诉内容");

        when(violationMapper.selectById(999L)).thenReturn(null);

        assertThrows(MerchantBusinessException.class, () -> violationService.appeal(request));
    }

    @Test
    @DisplayName("商家申诉：无权申诉应抛异常")
    void testAppeal_Unauthorized() {
        ViolationAppealRequest request = new ViolationAppealRequest();
        request.setViolationId(1L);
        request.setMerchantId(999L);
        request.setAppealContent("申诉内容");

        when(violationMapper.selectById(1L)).thenReturn(violation);

        assertThrows(MerchantBusinessException.class, () -> violationService.appeal(request));
    }

    @Test
    @DisplayName("商家申诉：非已处罚状态应抛异常")
    void testAppeal_NotPunished() {
        ViolationAppealRequest request = new ViolationAppealRequest();
        request.setViolationId(1L);
        request.setMerchantId(1L);
        request.setAppealContent("申诉内容");

        violation.setStatus(ViolationConstants.STATUS_PENDING);
        when(violationMapper.selectById(1L)).thenReturn(violation);

        assertThrows(MerchantBusinessException.class, () -> violationService.appeal(request));
    }

    @Test
    @DisplayName("商家申诉：已申诉应抛异常")
    void testAppeal_AlreadyAppealed() {
        ViolationAppealRequest request = new ViolationAppealRequest();
        request.setViolationId(1L);
        request.setMerchantId(1L);
        request.setAppealContent("申诉内容");

        violation.setStatus(ViolationConstants.STATUS_PUNISHED);
        violation.setIsAppealed(ViolationConstants.APPEAL_YES);
        when(violationMapper.selectById(1L)).thenReturn(violation);

        assertThrows(MerchantBusinessException.class, () -> violationService.appeal(request));
    }

    @Test
    @DisplayName("商家申诉：成功申诉")
    void testAppeal_Success() {
        ViolationAppealRequest request = new ViolationAppealRequest();
        request.setViolationId(1L);
        request.setMerchantId(1L);
        request.setAppealContent("申诉内容：本次违规系误判，请求撤销");

        violation.setStatus(ViolationConstants.STATUS_PUNISHED);
        violation.setIsAppealed(ViolationConstants.APPEAL_NO);
        when(violationMapper.selectById(1L)).thenReturn(violation);
        when(violationMapper.updateById(any(MerchantViolation.class))).thenReturn(1);

        MerchantViolation result = violationService.appeal(request);

        assertNotNull(result);
        assertEquals(ViolationConstants.STATUS_APPEALED, result.getStatus());
        assertEquals(ViolationConstants.APPEAL_YES, result.getIsAppealed());
        assertNotNull(result.getAppealContent());
        assertNotNull(result.getAppealTime());
    }

    @Test
    @DisplayName("处理申诉：违规记录不存在应抛异常")
    void testHandleAppeal_ViolationNotFound() {
        when(violationMapper.selectById(999L)).thenReturn(null);

        assertThrows(MerchantBusinessException.class,
            () -> violationService.handleAppeal(999L, true, "通过", 1L));
    }

    @Test
    @DisplayName("处理申诉：无申诉待处理应抛异常")
    void testHandleAppeal_NoAppeal() {
        violation.setStatus(ViolationConstants.STATUS_PUNISHED);
        when(violationMapper.selectById(1L)).thenReturn(violation);

        assertThrows(MerchantBusinessException.class,
            () -> violationService.handleAppeal(1L, true, "通过", 1L));
    }

    @Test
    @DisplayName("处理申诉：通过申诉撤销处罚")
    void testHandleAppeal_Approved() {
        violation.setStatus(ViolationConstants.STATUS_APPEALED);
        violation.setViolationLevel(ViolationConstants.LEVEL_GENERAL);
        when(violationMapper.selectById(1L)).thenReturn(violation);
        when(violationMapper.updateById(any(MerchantViolation.class))).thenReturn(1);
        when(merchantMapper.selectById(1L)).thenReturn(merchant);
        when(merchantMapper.updateById(any(Merchant.class))).thenReturn(1);
        when(violationMapper.selectMaxActivePunishmentType(1L)).thenReturn(null);

        MerchantViolation result = violationService.handleAppeal(1L, true, "申诉通过", 1L);

        assertNotNull(result);
        assertEquals(ViolationConstants.STATUS_REVOKED, result.getStatus());
        assertNull(result.getPunishmentStart());
        assertNull(result.getPunishmentEnd());
    }

    @Test
    @DisplayName("处理申诉：不通过申诉维持处罚")
    void testHandleAppeal_Rejected() {
        violation.setStatus(ViolationConstants.STATUS_APPEALED);
        when(violationMapper.selectById(1L)).thenReturn(violation);
        when(violationMapper.updateById(any(MerchantViolation.class))).thenReturn(1);

        MerchantViolation result = violationService.handleAppeal(1L, false, "申诉不通过", 1L);

        assertNotNull(result);
        assertEquals(ViolationConstants.STATUS_PUNISHED, result.getStatus());
    }

    @Test
    @DisplayName("撤销违规：成功撤销")
    void testRevoke_Success() {
        when(violationMapper.selectById(1L)).thenReturn(violation);
        when(violationMapper.updateById(any(MerchantViolation.class))).thenReturn(1);
        when(merchantMapper.selectById(1L)).thenReturn(merchant);
        when(merchantMapper.updateById(any(Merchant.class))).thenReturn(1);
        when(violationMapper.selectMaxActivePunishmentType(1L)).thenReturn(null);

        boolean result = violationService.revoke(1L, "撤销原因", 1L);

        assertTrue(result);
        assertEquals(ViolationConstants.STATUS_REVOKED, violation.getStatus());
    }

    @Test
    @DisplayName("撤销违规：记录不存在应抛异常")
    void testRevoke_NotFound() {
        when(violationMapper.selectById(999L)).thenReturn(null);

        assertThrows(MerchantBusinessException.class, () -> violationService.revoke(999L, "reason", 1L));
    }

    @Test
    @DisplayName("解除处罚：违规记录不存在返回false")
    void testRelease_ViolationNotFound() {
        when(violationMapper.selectById(999L)).thenReturn(null);

        boolean result = violationService.release(999L);

        assertFalse(result);
    }

    @Test
    @DisplayName("解除处罚：非已处罚状态返回false")
    void testRelease_InvalidStatus() {
        violation.setStatus(ViolationConstants.STATUS_PENDING);
        when(violationMapper.selectById(1L)).thenReturn(violation);

        boolean result = violationService.release(1L);

        assertFalse(result);
    }

    @Test
    @DisplayName("解除处罚：成功解除")
    void testRelease_Success() {
        violation.setStatus(ViolationConstants.STATUS_PUNISHED);
        when(violationMapper.selectById(1L)).thenReturn(violation);
        when(violationMapper.updateById(any(MerchantViolation.class))).thenReturn(1);
        when(merchantMapper.selectById(1L)).thenReturn(merchant);
        when(merchantMapper.updateById(any(Merchant.class))).thenReturn(1);
        when(violationMapper.selectMaxActivePunishmentType(1L)).thenReturn(null);

        boolean result = violationService.release(1L);

        assertTrue(result);
        assertEquals(ViolationConstants.STATUS_RELEASED, violation.getStatus());
    }

    @Test
    @DisplayName("查询商家违规记录：成功返回")
    void testListByMerchant_Success() {
        List<MerchantViolation> violations = Arrays.asList(violation);
        when(violationMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(violations);

        List<MerchantViolation> result = violationService.listByMerchant(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("统计商家违规次数：成功返回")
    void testCountByMerchantAndDateRange_Success() {
        when(violationMapper.countByMerchantAndDateRange(eq(1L), anyString(), anyString())).thenReturn(5L);

        long result = violationService.countByMerchantAndDateRange(1L, "2026-01-01", "2026-12-31");

        assertEquals(5L, result);
    }

    @Test
    @DisplayName("统计商家违规次数：null返回0")
    void testCountByMerchantAndDateRange_Null() {
        when(violationMapper.countByMerchantAndDateRange(eq(1L), anyString(), anyString())).thenReturn(null);

        long result = violationService.countByMerchantAndDateRange(1L, null, null);

        assertEquals(0L, result);
    }

    @Test
    @DisplayName("统计活跃处罚：成功返回")
    void testCountActivePunishment_Success() {
        when(violationMapper.countActivePunishment(1L)).thenReturn(2L);

        long result = violationService.countActivePunishment(1L);

        assertEquals(2L, result);
    }

    @Test
    @DisplayName("统计活跃处罚：null返回0")
    void testCountActivePunishment_Null() {
        when(violationMapper.countActivePunishment(1L)).thenReturn(null);

        long result = violationService.countActivePunishment(1L);

        assertEquals(0L, result);
    }

    @Test
    @DisplayName("获取最高活跃处罚类型：成功返回")
    void testGetMaxActivePunishmentType_Success() {
        when(violationMapper.selectMaxActivePunishmentType(1L)).thenReturn(ViolationConstants.PUNISH_LIMIT);

        Integer result = violationService.getMaxActivePunishmentType(1L);

        assertEquals(ViolationConstants.PUNISH_LIMIT, result);
    }

    @Test
    @DisplayName("检查是否被封禁：是")
    void testIsBanned_True() {
        when(violationMapper.selectMaxActivePunishmentType(1L)).thenReturn(ViolationConstants.PUNISH_BAN);

        boolean result = violationService.isBanned(1L);

        assertTrue(result);
    }

    @Test
    @DisplayName("检查是否被封禁：否")
    void testIsBanned_False() {
        when(violationMapper.selectMaxActivePunishmentType(1L)).thenReturn(ViolationConstants.PUNISH_LIMIT);

        boolean result = violationService.isBanned(1L);

        assertFalse(result);
    }

    @Test
    @DisplayName("检查是否被封禁：null")
    void testIsBanned_Null() {
        when(violationMapper.selectMaxActivePunishmentType(1L)).thenReturn(null);

        boolean result = violationService.isBanned(1L);

        assertFalse(result);
    }

    @Test
    @DisplayName("检查是否被限流：是")
    void testIsLimited_True() {
        when(violationMapper.selectMaxActivePunishmentType(1L)).thenReturn(ViolationConstants.PUNISH_LIMIT);

        boolean result = violationService.isLimited(1L);

        assertTrue(result);
    }

    @Test
    @DisplayName("检查是否被限流：否")
    void testIsLimited_False() {
        when(violationMapper.selectMaxActivePunishmentType(1L)).thenReturn(ViolationConstants.PUNISH_WARN);

        boolean result = violationService.isLimited(1L);

        assertFalse(result);
    }

    @Test
    @DisplayName("获取违规统计：成功返回")
    void testGetViolationStats_Success() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("total_count", 10L);
        stats.put("minor_count", 5L);
        stats.put("general_count", 3L);
        stats.put("serious_count", 1L);
        stats.put("very_serious_count", 1L);

        when(violationMapper.sumViolationStats(1L)).thenReturn(stats);
        when(violationMapper.countActivePunishment(1L)).thenReturn(1L);
        when(violationMapper.selectMaxActivePunishmentType(1L)).thenReturn(ViolationConstants.PUNISH_LIMIT);

        Map<String, Object> result = violationService.getViolationStats(1L);

        assertNotNull(result);
        assertEquals(10L, result.get("totalCount"));
        assertEquals(5L, result.get("minorCount"));
        assertEquals(3L, result.get("generalCount"));
        assertEquals(1L, result.get("seriousCount"));
        assertEquals(1L, result.get("verySeriousCount"));
        assertEquals(1L, result.get("activePunishment"));
        assertTrue((Boolean) result.get("isLimited"));
        assertFalse((Boolean) result.get("isBanned"));
    }

    @Test
    @DisplayName("获取违规统计：无数据返回默认值")
    void testGetViolationStats_Empty() {
        when(violationMapper.sumViolationStats(1L)).thenReturn(null);
        when(violationMapper.countActivePunishment(1L)).thenReturn(0L);
        when(violationMapper.selectMaxActivePunishmentType(1L)).thenReturn(null);

        Map<String, Object> result = violationService.getViolationStats(1L);

        assertNotNull(result);
        assertEquals(0L, result.get("totalCount"));
        assertEquals(0L, result.get("deductedScore"));
        assertEquals(100L, result.get("currentScore"));
    }

    @Test
    @DisplayName("获取违规统计：字符串类型数值解析")
    void testGetViolationStats_StringValues() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("total_count", "10");
        stats.put("minor_count", "5");

        when(violationMapper.sumViolationStats(1L)).thenReturn(stats);
        when(violationMapper.countActivePunishment(1L)).thenReturn(0L);
        when(violationMapper.selectMaxActivePunishmentType(1L)).thenReturn(null);

        Map<String, Object> result = violationService.getViolationStats(1L);
        assertNotNull(result);
    }

    @Test
    @DisplayName("处罚扣分：轻微5分")
    void testDeductScore_Minor() {
        violation.setViolationLevel(ViolationConstants.LEVEL_MINOR);
        when(violationMapper.selectById(1L)).thenReturn(violation);
        when(violationMapper.updateById(any(MerchantViolation.class))).thenReturn(1);
        when(merchantMapper.selectById(1L)).thenReturn(merchant);
        when(merchantMapper.updateById(any(Merchant.class))).thenReturn(1);

        ViolationPunishRequest request = new ViolationPunishRequest();
        request.setViolationId(1L);
        request.setViolationLevel(ViolationConstants.LEVEL_MINOR);
        request.setPunishmentType(ViolationConstants.PUNISH_WARN);
        request.setPunishmentDays(0);
        request.setHandlerId(1L);

        violationService.punish(request);
        assertEquals(95, merchant.getViolationScore());
    }

    @Test
    @DisplayName("处罚扣分：商家violationScore为null使用满分100")
    void testDeductScore_NullScore() {
        merchant.setViolationScore(null);
        violation.setViolationLevel(ViolationConstants.LEVEL_GENERAL);
        when(violationMapper.selectById(1L)).thenReturn(violation);
        when(violationMapper.updateById(any(MerchantViolation.class))).thenReturn(1);
        when(merchantMapper.selectById(1L)).thenReturn(merchant);
        when(merchantMapper.updateById(any(Merchant.class))).thenReturn(1);

        ViolationPunishRequest request = new ViolationPunishRequest();
        request.setViolationId(1L);
        request.setViolationLevel(ViolationConstants.LEVEL_GENERAL);
        request.setPunishmentType(ViolationConstants.PUNISH_WARN);
        request.setPunishmentDays(0);
        request.setHandlerId(1L);

        violationService.punish(request);
        assertEquals(85, merchant.getViolationScore());
    }
}
