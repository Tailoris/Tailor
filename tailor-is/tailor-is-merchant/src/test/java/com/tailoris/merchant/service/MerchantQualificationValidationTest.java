package com.tailoris.merchant.service;

import com.tailoris.merchant.entity.MerchantQualification;
import com.tailoris.merchant.exception.MerchantBusinessException;
import com.tailoris.merchant.service.impl.MerchantQualificationServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 商家资质上传校验单元测试 - MER-009.
 *
 * @author Tailor IS Team
 * @since 2026-06-03
 */
@DisplayName("商家资质上传校验单元测试")
class MerchantQualificationValidationTest {

    private final MerchantQualificationServiceImpl service = new MerchantQualificationServiceImpl(null);

    @Test
    @DisplayName("统一社会信用代码18位格式校验")
    void testCreditCodeValid() {
        assertTrue(matchesCreditCode("91110000600037341L"));
        assertTrue(matchesCreditCode("91440300MA5DA8QK3X"));
        assertFalse(matchesCreditCode("12345"));
        assertFalse(matchesCreditCode("91110000600037341"));
        assertFalse(matchesCreditCode("911100006000373411L"));
        assertFalse(matchesCreditCode("91110000600037341i"));
    }

    @Test
    @DisplayName("身份证号18位格式校验")
    void testIdCardValid() {
        assertTrue(matchesIdCard("11010119900307851X"));
        assertTrue(matchesIdCard("44030719880101001X"));
        assertFalse(matchesIdCard("12345"));
        assertTrue(matchesIdCard("110101199003078511"));
        assertFalse(matchesIdCard("01010119900307851X"));
    }

    @Test
    @DisplayName("身份证号校验公开方法")
    void testValidateIdCardPublic() {
        assertTrue(service.validateIdCard("11010119900307851X"));
        assertFalse(service.validateIdCard("12345"));
        assertFalse(service.validateIdCard(null));
    }

    @Test
    @DisplayName("资质类型不能为空")
    void testQualificationTypeRequired() {
        MerchantQualification q = new MerchantQualification();
        q.setCertUrl("https://example.com/license.jpg");
        q.setCertType(null);
        assertThrows(MerchantBusinessException.class, () -> service.uploadQualification(1L, q));
    }

    @Test
    @DisplayName("资质文件URL不能为空")
    void testFileUrlRequired() {
        MerchantQualification q = new MerchantQualification();
        q.setCertType(1);
        q.setCertUrl(null);
        assertThrows(MerchantBusinessException.class, () -> service.uploadQualification(1L, q));
    }

    @Test
    @DisplayName("资质文件URL格式校验")
    void testFileUrlFormat() {
        MerchantQualification q = new MerchantQualification();
        q.setCertType(1);
        q.setCertUrl("invalid-url");
        assertThrows(MerchantBusinessException.class, () -> service.uploadQualification(1L, q));
    }

    @Test
    @DisplayName("统一社会信用代码格式校验-错误")
    void testInvalidCreditCode() {
        MerchantQualification q = new MerchantQualification();
        q.setCertType(1);
        q.setCertUrl("https://example.com/license.jpg");
        q.setCertNo("12345");
        assertThrows(MerchantBusinessException.class, () -> service.uploadQualification(1L, q));
    }

    // ============================================================
    // 辅助方法
    // ============================================================

    private boolean matchesCreditCode(String code) {
        return code != null && code.matches("[0-9A-HJ-NPQRTUWXY]{18}");
    }

    private boolean matchesIdCard(String id) {
        return id != null && id.matches("[1-9]\\d{5}(?:18|19|20)\\d{2}(?:0\\d|1[0-2])(?:[0-2]\\d|3[01])\\d{3}[0-9Xx]");
    }
}
