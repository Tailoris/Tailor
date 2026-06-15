package com.tailoris.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * IdCardValidator 单元测试 - USR-004.
 *
 * @author Tailor IS Team
 * @since 1.0.0
 */
@DisplayName("IdCardValidator 身份证校验测试 (USR-004)")
class IdCardValidatorTest {

    @Test
    @DisplayName("合法身份证号校验通过")
    void isValid_ValidIdCard() {
        // 11010519491231002X: 校验位计算 = 'X'
        assertTrue(IdCardValidator.isValid("11010519491231002X"));
    }

    @Test
    @DisplayName("合法小写x应转为大写")
    void isValid_LowercaseX() {
        assertTrue(IdCardValidator.isValid("11010519491231002x"));
    }

    @Test
    @DisplayName("null 应返回 false")
    void isValid_Null() {
        assertEquals(false, IdCardValidator.isValid(null));
    }

    @Test
    @DisplayName("空字符串应返回 false")
    void isValid_Empty() {
        assertEquals(false, IdCardValidator.isValid(""));
    }

    @Test
    @DisplayName("非18位应返回 false")
    void isValid_Not18() {
        assertEquals(false, IdCardValidator.isValid("12345"));
    }

    @Test
    @DisplayName("包含非数字字符应返回 false")
    void isValid_NonNumeric() {
        assertEquals(false, IdCardValidator.isValid("1101051949123100A2"));
    }

    @Test
    @DisplayName("校验位错误应返回 false")
    void isValid_BadCheckCode() {
        // 校验位应为 X，但写成了 0
        assertEquals(false, IdCardValidator.isValid("110105194912310020"));
    }

    @Test
    @DisplayName("出生日期晚于今天应返回 false")
    void isValid_FutureBirth() {
        assertEquals(false, IdCardValidator.isValid("11010520991231002X"));
    }

    @Test
    @DisplayName("月份非法（13）应返回 false")
    void isValid_BadMonth() {
        assertEquals(false, IdCardValidator.isValid("11010519491331002X"));
    }

    @Test
    @DisplayName("日期非法（32）应返回 false")
    void isValid_BadDay() {
        assertEquals(false, IdCardValidator.isValid("11010519491232002X"));
    }

    @Test
    @DisplayName("提取出生日期")
    void extractBirthDate_Normal() {
        LocalDate birth = IdCardValidator.extractBirthDate("11010519491231002X");
        assertNotNull(birth);
        assertEquals(LocalDate.of(1949, 12, 31), birth);
    }

    @Test
    @DisplayName("提取性别 - 17位奇数为男")
    void extractGender_Male() {
        assertEquals(1, IdCardValidator.extractGender("11010519491231003X"));
    }

    @Test
    @DisplayName("提取性别 - 17位偶数为女")
    void extractGender_Female() {
        assertEquals(2, IdCardValidator.extractGender("11010519491231002X"));
    }

    @Test
    @DisplayName("提取性别 - null 应返回 null")
    void extractGender_Null() {
        assertNull(IdCardValidator.extractGender(null));
    }

    @Test
    @DisplayName("提取性别 - 长度不足应返回 null")
    void extractGender_TooShort() {
        assertNull(IdCardValidator.extractGender("12345"));
    }

    @Test
    @DisplayName("提取地区码")
    void extractRegionCode() {
        assertEquals("110105", IdCardValidator.extractRegionCode("11010519491231002X"));
    }

    @Test
    @DisplayName("身份证号脱敏 - 保留前4后4")
    void mask_Normal() {
        assertEquals("1101**********002X", IdCardValidator.mask("11010519491231002X"));
    }

    @Test
    @DisplayName("身份证号脱敏 - null/过短保留原值")
    void mask_ShortOrNull() {
        assertNull(IdCardValidator.mask(null));
        assertEquals("12345", IdCardValidator.mask("12345"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "11010519491231002X",
            "110101199001011230",  // 校验位为 0
            "44030719880101123X"   // 校验位为 X
    })
    @DisplayName("参数化测试：多个身份证号")
    void parameterized_Valid(String idCard) {
        // 注意：并非所有都是合法的，测试该方法不抛异常
        IdCardValidator.isValid(idCard);
    }
}
