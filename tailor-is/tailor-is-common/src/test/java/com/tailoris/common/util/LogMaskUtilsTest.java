package com.tailoris.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("LogMaskUtils 测试")
class LogMaskUtilsTest {

    @Nested
    @DisplayName("手机号脱敏测试")
    class MaskPhoneTests {

        @Test
        @DisplayName("正常手机号脱敏")
        void testMaskPhone() {
            assertEquals("138****5678", LogMaskUtils.maskPhone("13812345678"));
        }

        @Test
        @DisplayName("null 返回 null")
        void testMaskPhoneNull() {
            assertNull(LogMaskUtils.maskPhone(null));
        }

        @Test
        @DisplayName("长度小于 7 返回原值")
        void testMaskPhoneShort() {
            assertEquals("123456", LogMaskUtils.maskPhone("123456"));
        }
    }

    @Nested
    @DisplayName("邮箱脱敏测试")
    class MaskEmailTests {

        @Test
        @DisplayName("正常邮箱脱敏")
        void testMaskEmail() {
            assertEquals("t***@example.com", LogMaskUtils.maskEmail("test@example.com"));
        }

        @Test
        @DisplayName("短前缀邮箱脱敏")
        void testMaskEmailShortPrefix() {
            assertEquals("*@example.com", LogMaskUtils.maskEmail("te@example.com"));
        }

        @Test
        @DisplayName("null 返回 null")
        void testMaskEmailNull() {
            assertNull(LogMaskUtils.maskEmail(null));
        }

        @Test
        @DisplayName("无 @ 符号返回原值")
        void testMaskEmailNoAt() {
            assertEquals("test", LogMaskUtils.maskEmail("test"));
        }
    }

    @Nested
    @DisplayName("身份证号脱敏测试")
    class MaskIdCardTests {

        @Test
        @DisplayName("正常身份证号脱敏")
        void testMaskIdCard() {
            assertEquals("1101**********1234", LogMaskUtils.maskIdCard("110101199001011234"));
        }

        @Test
        @DisplayName("null 返回 null")
        void testMaskIdCardNull() {
            assertNull(LogMaskUtils.maskIdCard(null));
        }

        @Test
        @DisplayName("长度小于 8 返回原值")
        void testMaskIdCardShort() {
            assertEquals("1234567", LogMaskUtils.maskIdCard("1234567"));
        }
    }

    @Nested
    @DisplayName("银行卡号脱敏测试")
    class MaskBankCardTests {

        @Test
        @DisplayName("正常银行卡号脱敏")
        void testMaskBankCard() {
            assertEquals("6222 **** **** 5678", LogMaskUtils.maskBankCard("6222020200005678"));
        }

        @Test
        @DisplayName("null 返回 null")
        void testMaskBankCardNull() {
            assertNull(LogMaskUtils.maskBankCard(null));
        }

        @Test
        @DisplayName("长度小于 8 返回原值")
        void testMaskBankCardShort() {
            assertEquals("1234567", LogMaskUtils.maskBankCard("1234567"));
        }
    }

    @Nested
    @DisplayName("姓名脱敏测试")
    class MaskNameTests {

        @Test
        @DisplayName("单字姓名脱敏")
        void testMaskNameSingle() {
            assertEquals("*", LogMaskUtils.maskName("张"));
        }

        @Test
        @DisplayName("双字姓名脱敏")
        void testMaskNameDouble() {
            assertEquals("张*", LogMaskUtils.maskName("张三"));
        }

        @Test
        @DisplayName("多字姓名脱敏")
        void testMaskNameMultiple() {
            assertEquals("张*三", LogMaskUtils.maskName("张小三"));
            assertEquals("欧**文", LogMaskUtils.maskName("欧阳修文"));
        }

        @Test
        @DisplayName("null 返回 null")
        void testMaskNameNull() {
            assertNull(LogMaskUtils.maskName(null));
        }

        @Test
        @DisplayName("空字符串返回空字符串")
        void testMaskNameEmpty() {
            assertEquals("", LogMaskUtils.maskName(""));
        }
    }

    @Nested
    @DisplayName("订单号脱敏测试")
    class MaskOrderNoTests {

        @Test
        @DisplayName("正常订单号脱敏")
        void testMaskOrderNo() {
            assertEquals("ORD***789", LogMaskUtils.maskOrderNo("ORDER123789"));
        }

        @Test
        @DisplayName("null 返回 null")
        void testMaskOrderNoNull() {
            assertNull(LogMaskUtils.maskOrderNo(null));
        }

        @Test
        @DisplayName("长度小于等于 6 返回原值")
        void testMaskOrderNoShort() {
            assertEquals("ORDER1", LogMaskUtils.maskOrderNo("ORDER1"));
        }
    }

    @Nested
    @DisplayName("通用字符串脱敏测试")
    class MaskStringTests {

        @Test
        @DisplayName("正常字符串脱敏")
        void testMaskString() {
            assertEquals("te****34", LogMaskUtils.maskString("test1234"));
        }

        @Test
        @DisplayName("短字符串脱敏")
        void testMaskStringShort() {
            assertEquals("****", LogMaskUtils.maskString("test"));
        }

        @Test
        @DisplayName("null 返回 null")
        void testMaskStringNull() {
            assertNull(LogMaskUtils.maskString(null));
        }

        @Test
        @DisplayName("空字符串返回空字符串")
        void testMaskStringEmpty() {
            assertEquals("", LogMaskUtils.maskString(""));
        }
    }
}
