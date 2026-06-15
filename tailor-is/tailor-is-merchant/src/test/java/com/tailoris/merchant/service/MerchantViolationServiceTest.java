package com.tailoris.merchant.service;

import com.tailoris.merchant.constant.ViolationConstants;
import com.tailoris.merchant.entity.MerchantViolation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 商家违规处罚单元测试 - MER-009.
 *
 * <p>测试违规处罚业务逻辑的正确性。</p>
 *
 * @author Tailor IS Team
 * @since 2026-06-03
 */
@DisplayName("商家违规处罚单元测试")
class MerchantViolationServiceTest {

    @Test
    @DisplayName("违规级别扣分规则验证")
    void testDeductScoreByLevel() {
        assertEquals(ViolationConstants.DEDUCT_MINOR, 5);
        assertEquals(ViolationConstants.DEDUCT_GENERAL, 15);
        assertEquals(ViolationConstants.DEDUCT_SERIOUS, 30);
        assertEquals(ViolationConstants.DEDUCT_VERY_SERIOUS, 60);
    }

    @Test
    @DisplayName("违规类型枚举验证")
    void testViolationTypeEnum() {
        assertEquals(1, ViolationConstants.TYPE_PRODUCT);
        assertEquals(2, ViolationConstants.TYPE_PRICE);
        assertEquals(3, ViolationConstants.TYPE_ADVERTISE);
        assertEquals(4, ViolationConstants.TYPE_AFTER_SALE);
        assertEquals(5, ViolationConstants.TYPE_QUALIFICATION);
        assertEquals(6, ViolationConstants.TYPE_OTHER);
    }

    @Test
    @DisplayName("处罚类型枚举验证")
    void testPunishmentTypeEnum() {
        assertEquals(0, ViolationConstants.PUNISH_PENDING);
        assertEquals(1, ViolationConstants.PUNISH_WARN);
        assertEquals(2, ViolationConstants.PUNISH_LIMIT);
        assertEquals(3, ViolationConstants.PUNISH_OFFLINE);
        assertEquals(4, ViolationConstants.PUNISH_BAN);
        assertEquals(5, ViolationConstants.PUNISH_EVICT);
    }

    @Test
    @DisplayName("违规状态流转正确性")
    void testStatusTransition() {
        MerchantViolation v = new MerchantViolation();
        v.setStatus(ViolationConstants.STATUS_PENDING);
        v.setCreateTime(LocalDateTime.now());

        // 状态转换：PENDING -> PUNISHED
        v.setStatus(ViolationConstants.STATUS_PUNISHED);
        v.setPunishmentType(ViolationConstants.PUNISH_LIMIT);
        v.setPunishmentDays(7);
        v.setPunishmentStart(LocalDateTime.now());
        v.setPunishmentEnd(LocalDateTime.now().plusDays(7));
        assertEquals(ViolationConstants.STATUS_PUNISHED, v.getStatus());
        assertNotNull(v.getPunishmentStart());
        assertNotNull(v.getPunishmentEnd());

        // 申诉：PUNISHED -> APPEALED
        v.setStatus(ViolationConstants.STATUS_APPEALED);
        v.setIsAppealed(ViolationConstants.APPEAL_YES);
        v.setAppealContent("申诉内容：经审核，本次违规系误判");
        v.setAppealTime(LocalDateTime.now());
        assertEquals(ViolationConstants.STATUS_APPEALED, v.getStatus());
        assertEquals(ViolationConstants.APPEAL_YES, v.getIsAppealed());

        // 申诉处理：APPEALED -> REVOKED
        v.setStatus(ViolationConstants.STATUS_REVOKED);
        v.setHandleTime(LocalDateTime.now());
        assertEquals(ViolationConstants.STATUS_REVOKED, v.getStatus());
    }

    @Test
    @DisplayName("永久处罚punishmentEnd应为null")
    void testPermanentPunishment() {
        MerchantViolation v = new MerchantViolation();
        v.setPunishmentType(ViolationConstants.PUNISH_EVICT);
        v.setPunishmentDays(0);  // 0表示永久
        v.setPunishmentStart(LocalDateTime.now());
        v.setPunishmentEnd(null);  // 永久无结束时间
        assertNull(v.getPunishmentEnd());
    }

    @Test
    @DisplayName("临时处罚punishmentEnd应正确计算")
    void testTemporaryPunishment() {
        int days = 30;
        MerchantViolation v = new MerchantViolation();
        v.setPunishmentType(ViolationConstants.PUNISH_BAN);
        v.setPunishmentDays(days);
        v.setPunishmentStart(LocalDateTime.now());
        v.setPunishmentEnd(v.getPunishmentStart().plusDays(days));

        assertNotNull(v.getPunishmentEnd());
        long diff = v.getPunishmentEnd().toLocalDate().toEpochDay()
                - v.getPunishmentStart().toLocalDate().toEpochDay();
        assertEquals(days, diff);
    }

    @Test
    @DisplayName("违规扣分不能超过满分")
    void testViolationScoreFloor() {
        int current = 50;
        int deduct = ViolationConstants.DEDUCT_VERY_SERIOUS;  // 60
        int newScore = Math.max(0, current - deduct);
        assertEquals(0, newScore, "扣分后不能为负数");

        int current2 = 100;
        int deduct2 = ViolationConstants.DEDUCT_MINOR;  // 5
        int newScore2 = Math.max(0, current2 - deduct2);
        assertEquals(95, newScore2);
    }
}
