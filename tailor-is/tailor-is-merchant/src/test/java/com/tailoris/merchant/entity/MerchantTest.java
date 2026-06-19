package com.tailoris.merchant.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("商户实体测试")
class MerchantTest {

    @Test
    @DisplayName("创建商户 - 基本属性")
    void testMerchant_BasicProperties() {
        Merchant merchant = new Merchant();
        merchant.setId(1L);
        merchant.setUserId(100L);
        merchant.setMerchantType(1);
        merchant.setCompanyName("北京裁智云科技有限公司");
        merchant.setLicenseNo("91110105MA12345678");
        merchant.setContactName("张三");
        merchant.setContactPhone("13800138000");
        merchant.setContactEmail("zhangsan@tailoris.com");

        assertEquals(1L, merchant.getId());
        assertEquals(100L, merchant.getUserId());
        assertEquals(1, merchant.getMerchantType());
        assertEquals("北京裁智云科技有限公司", merchant.getCompanyName());
        assertEquals("91110105MA12345678", merchant.getLicenseNo());
        assertEquals("张三", merchant.getContactName());
        assertEquals("13800138000", merchant.getContactPhone());
        assertEquals("zhangsan@tailoris.com", merchant.getContactEmail());
    }

    @Test
    @DisplayName("创建商户 - 地址信息")
    void testMerchant_AddressInfo() {
        Merchant merchant = new Merchant();
        merchant.setProvince("北京市");
        merchant.setCity("北京市");
        merchant.setDistrict("朝阳区");
        merchant.setAddress("建国路88号SOHO现代城A座1001");

        assertEquals("北京市", merchant.getProvince());
        assertEquals("北京市", merchant.getCity());
        assertEquals("朝阳区", merchant.getDistrict());
        assertEquals("建国路88号SOHO现代城A座1001", merchant.getAddress());
    }

    @Test
    @DisplayName("创建商户 - 经营范围")
    void testMerchant_BusinessScope() {
        Merchant merchant = new Merchant();
        merchant.setBusinessScope("服装定制、面料销售、设计服务");

        assertEquals("服装定制、面料销售、设计服务", merchant.getBusinessScope());
    }

    @Test
    @DisplayName("创建商户 - 状态信息")
    void testMerchant_StatusInfo() {
        Merchant merchant = new Merchant();
        merchant.setStatus(1);
        merchant.setAuditStatus(2);
        merchant.setAuditRemark("审核通过");
        merchant.setAuditTime(LocalDateTime.now());
        merchant.setAuditBy(1L);

        assertEquals(1, merchant.getStatus());
        assertEquals(2, merchant.getAuditStatus());
        assertEquals("审核通过", merchant.getAuditRemark());
        assertNotNull(merchant.getAuditTime());
        assertEquals(1L, merchant.getAuditBy());
    }

    @Test
    @DisplayName("创建商户 - 入驻时间")
    void testMerchant_JoinTime() {
        Merchant merchant = new Merchant();
        LocalDateTime joinTime = LocalDateTime.now();
        LocalDateTime expireTime = joinTime.plusYears(1);
        merchant.setJoinTime(joinTime);
        merchant.setExpireTime(expireTime);

        assertEquals(joinTime, merchant.getJoinTime());
        assertEquals(expireTime, merchant.getExpireTime());
    }

    @Test
    @DisplayName("创建商户 - 试运营信息")
    void testMerchant_TrialInfo() {
        Merchant merchant = new Merchant();
        merchant.setIsTrial(1);
        merchant.setTrialStartDate("2026-06-01");
        merchant.setTrialEndDate("2026-06-30");

        assertEquals(1, merchant.getIsTrial());
        assertEquals("2026-06-01", merchant.getTrialStartDate());
        assertEquals("2026-06-30", merchant.getTrialEndDate());
    }

    @Test
    @DisplayName("创建商户 - 转正信息")
    void testMerchant_PromoteInfo() {
        Merchant merchant = new Merchant();
        merchant.setIsPromoted(1);
        merchant.setPromoteTime(LocalDateTime.now());

        assertEquals(1, merchant.getIsPromoted());
        assertNotNull(merchant.getPromoteTime());
    }

    @Test
    @DisplayName("创建商户 - 违规扣分")
    void testMerchant_ViolationScore() {
        Merchant merchant = new Merchant();
        merchant.setViolationScore(0);

        assertEquals(0, merchant.getViolationScore());
    }

    @Test
    @DisplayName("创建商户 - 违规扣分增加")
    void testMerchant_ViolationScoreIncrease() {
        Merchant merchant = new Merchant();
        merchant.setViolationScore(0);
        merchant.setViolationScore(merchant.getViolationScore() + 10);

        assertEquals(10, merchant.getViolationScore());
    }

    @Test
    @DisplayName("创建商户 - 违规扣分上限")
    void testMerchant_ViolationScoreMax() {
        Merchant merchant = new Merchant();
        merchant.setViolationScore(100);

        assertEquals(100, merchant.getViolationScore());
    }

    @Test
    @DisplayName("创建商户 - 处罚状态")
    void testMerchant_PunishmentStatus() {
        Merchant merchant = new Merchant();
        merchant.setPunishmentStatus(0);
        merchant.setPunishmentEnd(LocalDateTime.now().plusDays(7));

        assertEquals(0, merchant.getPunishmentStatus());
        assertNotNull(merchant.getPunishmentEnd());
    }

    @Test
    @DisplayName("创建商户 - 不同处罚状态")
    void testMerchant_DifferentPunishmentStatus() {
        Merchant merchant = new Merchant();

        // 正常
        merchant.setPunishmentStatus(0);
        assertEquals(0, merchant.getPunishmentStatus());

        // 限流
        merchant.setPunishmentStatus(1);
        assertEquals(1, merchant.getPunishmentStatus());

        // 下架
        merchant.setPunishmentStatus(2);
        assertEquals(2, merchant.getPunishmentStatus());

        // 封禁
        merchant.setPunishmentStatus(3);
        assertEquals(3, merchant.getPunishmentStatus());
    }

    @Test
    @DisplayName("创建商户 - 不同商户类型")
    void testMerchant_DifferentMerchantTypes() {
        Merchant merchant = new Merchant();

        // 个人商户
        merchant.setMerchantType(1);
        assertEquals(1, merchant.getMerchantType());

        // 企业商户
        merchant.setMerchantType(2);
        assertEquals(2, merchant.getMerchantType());
    }

    @Test
    @DisplayName("创建商户 - 不同审核状态")
    void testMerchant_DifferentAuditStatus() {
        Merchant merchant = new Merchant();

        // 待审核
        merchant.setAuditStatus(0);
        assertEquals(0, merchant.getAuditStatus());

        // 审核中
        merchant.setAuditStatus(1);
        assertEquals(1, merchant.getAuditStatus());

        // 审核通过
        merchant.setAuditStatus(2);
        assertEquals(2, merchant.getAuditStatus());

        // 审核拒绝
        merchant.setAuditStatus(3);
        assertEquals(3, merchant.getAuditStatus());
    }

    @Test
    @DisplayName("创建商户 - 不同状态")
    void testMerchant_DifferentStatus() {
        Merchant merchant = new Merchant();

        // 禁用
        merchant.setStatus(0);
        assertEquals(0, merchant.getStatus());

        // 启用
        merchant.setStatus(1);
        assertEquals(1, merchant.getStatus());
    }
}
