package com.tailoris.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("DesensitizeUtils 测试")
class DesensitizeUtilsTest {

    @Nested
    @DisplayName("手机号脱敏测试")
    class PhoneMaskingTests {

        @Test
        @DisplayName("正常手机号脱敏")
        void testMaskPhone() {
            String phone = "13800138000";
            String masked = DesensitizeUtils.maskPhone(phone);
            assertEquals("138****8000", masked);
        }

        @Test
        @DisplayName("null 手机号返回 null")
        void testMaskPhoneNull() {
            assertNull(DesensitizeUtils.maskPhone(null));
        }

        @Test
        @DisplayName("过短手机号不脱敏")
        void testMaskPhoneTooShort() {
            String phone = "123456";
            assertEquals(phone, DesensitizeUtils.maskPhone(phone));
        }

        @Test
        @DisplayName("空字符串手机号")
        void testMaskPhoneEmpty() {
            String phone = "";
            assertEquals(phone, DesensitizeUtils.maskPhone(phone));
        }
    }

    @Nested
    @DisplayName("身份证脱敏测试")
    class IdCardMaskingTests {

        @Test
        @DisplayName("正常身份证脱敏")
        void testMaskIdCard() {
            String idCard = "110101199001011234";
            String masked = DesensitizeUtils.maskIdCard(idCard);
            assertEquals("1101********1234", masked);
        }

        @Test
        @DisplayName("null 身份证返回 null")
        void testMaskIdCardNull() {
            assertNull(DesensitizeUtils.maskIdCard(null));
        }

        @Test
        @DisplayName("过短身份证号不脱敏")
        void testMaskIdCardTooShort() {
            String idCard = "123456789";
            assertEquals(idCard, DesensitizeUtils.maskIdCard(idCard));
        }
    }

    @Nested
    @DisplayName("银行卡脱敏测试")
    class BankCardMaskingTests {

        @Test
        @DisplayName("正常银行卡脱敏")
        void testMaskBankCard() {
            String bankCard = "6222021234567890123";
            String masked = DesensitizeUtils.maskBankCard(bankCard);
            assertEquals("6222****0123", masked);
        }

        @Test
        @DisplayName("null 银行卡返回 null")
        void testMaskBankCardNull() {
            assertNull(DesensitizeUtils.maskBankCard(null));
        }

        @Test
        @DisplayName("过短银行卡号不脱敏")
        void testMaskBankCardTooShort() {
            String bankCard = "1234567";
            assertEquals(bankCard, DesensitizeUtils.maskBankCard(bankCard));
        }
    }

    @Nested
    @DisplayName("邮箱脱敏测试")
    class EmailMaskingTests {

        @Test
        @DisplayName("正常邮箱脱敏")
        void testMaskEmail() {
            String email = "zhangsan@example.com";
            String masked = DesensitizeUtils.maskEmail(email);
            assertEquals("z****n@example.com", masked);
        }

        @Test
        @DisplayName("null 邮箱返回 null")
        void testMaskEmailNull() {
            assertNull(DesensitizeUtils.maskEmail(null));
        }

        @Test
        @DisplayName("无效邮箱格式不脱敏")
        void testMaskEmailInvalid() {
            String email = "invalid-email";
            assertEquals(email, DesensitizeUtils.maskEmail(email));
        }

        @Test
        @DisplayName("短邮箱用户名不脱敏")
        void testMaskEmailShortLocalPart() {
            String email = "ab@example.com";
            assertEquals(email, DesensitizeUtils.maskEmail(email));
        }
    }

    @Nested
    @DisplayName("姓名脱敏测试")
    class NameMaskingTests {

        @Test
        @DisplayName("三个字姓名脱敏")
        void testMaskNameThreeChars() {
            assertEquals("张**丰", DesensitizeUtils.maskName("张三丰"));
        }

        @Test
        @DisplayName("两个字姓名脱敏")
        void testMaskNameTwoChars() {
            assertEquals("张*", DesensitizeUtils.maskName("张三"));
        }

        @Test
        @DisplayName("单字姓名不脱敏")
        void testMaskNameOneChar() {
            assertEquals("张", DesensitizeUtils.maskName("张"));
        }

        @Test
        @DisplayName("null 姓名返回 null")
        void testMaskNameNull() {
            assertNull(DesensitizeUtils.maskName(null));
        }

        @Test
        @DisplayName("空字符串姓名")
        void testMaskNameEmpty() {
            assertEquals("", DesensitizeUtils.maskName(""));
        }

        @Test
        @DisplayName("长姓名脱敏")
        void testMaskNameLong() {
            assertEquals("阿**巴", DesensitizeUtils.maskName("阿里巴巴"));
        }
    }

    @Nested
    @DisplayName("地址脱敏测试")
    class AddressMaskingTests {

        @Test
        @DisplayName("正常地址脱敏")
        void testMaskAddress() {
            String address = "北京市朝阳区建国路88号";
            String masked = DesensitizeUtils.maskAddress(address);
            assertEquals("北京市朝阳区********", masked);
        }

        @Test
        @DisplayName("null 地址返回 null")
        void testMaskAddressNull() {
            assertNull(DesensitizeUtils.maskAddress(null));
        }

        @Test
        @DisplayName("过短地址不脱敏")
        void testMaskAddressTooShort() {
            String address = "北京";
            assertEquals(address, DesensitizeUtils.maskAddress(address));
        }
    }

    @Nested
    @DisplayName("自定义脱敏测试")
    class CustomMaskingTests {

        @Test
        @DisplayName("自定义脱敏")
        void testMaskCustom() {
            String input = "abcdef123456";
            String masked = DesensitizeUtils.maskCustom(input, 3, 3);
            assertEquals("abc******456", masked);
        }

        @Test
        @DisplayName("null 输入返回 null")
        void testMaskCustomNull() {
            assertNull(DesensitizeUtils.maskCustom(null, 2, 2));
        }

        @Test
        @DisplayName("输入长度不足返回原值")
        void testMaskCustomTooShort() {
            String input = "abc";
            assertEquals(input, DesensitizeUtils.maskCustom(input, 2, 2));
        }

        @Test
        @DisplayName("自定义脱敏前缀后缀")
        void testMaskCustomWithDifferentPrefixSuffix() {
            String input = "1234567890";
            String masked = DesensitizeUtils.maskCustom(input, 2, 4);
            assertEquals("12****7890", masked);
        }
    }
}
