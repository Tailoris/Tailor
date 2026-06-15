package com.tailoris.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("StringUtils 测试")
class StringUtilsTest {

    @Nested
    @DisplayName("空白判断测试")
    class BlankCheckTests {

        @Test
        @DisplayName("null 是空白")
        void testIsBlankNull() {
            assertTrue(StringUtils.isBlank((CharSequence) null));
        }

        @Test
        @DisplayName("空字符串是空白")
        void testIsBlankEmpty() {
            assertTrue(StringUtils.isBlank(""));
        }

        @Test
        @DisplayName("纯空格是空白")
        void testIsBlankWhitespace() {
            assertTrue(StringUtils.isBlank("   "));
        }

        @Test
        @DisplayName("非空白字符串")
        void testIsNotBlank() {
            assertTrue(StringUtils.isNotBlank("hello"));
        }

        @Test
        @DisplayName("Collection 空白判断")
        void testIsBlankCollection() {
            assertTrue(StringUtils.isBlank((List<String>) null));
            assertTrue(StringUtils.isBlank(new ArrayList<>()));
            assertTrue(StringUtils.isNotBlank(List.of("a")));
        }

        @Test
        @DisplayName("Map 空白判断")
        void testIsBlankMap() {
            assertTrue(StringUtils.isBlank((Map<String, String>) null));
            assertTrue(StringUtils.isBlank(new HashMap<>()));
            Map<String, String> map = new HashMap<>();
            map.put("key", "value");
            assertTrue(StringUtils.isNotBlank(map));
        }

        @Test
        @DisplayName("数组空白判断")
        void testIsBlankArray() {
            assertTrue(StringUtils.isBlank((Object[]) null));
            assertTrue(StringUtils.isBlank(new Object[0]));
            assertTrue(StringUtils.isNotBlank(new Object[]{"a"}));
        }
    }

    @Nested
    @DisplayName("空值判断测试")
    class EmptyCheckTests {

        @Test
        @DisplayName("null 是空")
        void testIsNullEmpty() {
            assertTrue(StringUtils.isEmpty(null));
        }

        @Test
        @DisplayName("空字符串是空")
        void testIsEmptyString() {
            assertTrue(StringUtils.isEmpty(""));
        }

        @Test
        @DisplayName("空格不是空")
        void testIsNotEmpty() {
            assertFalse(StringUtils.isEmpty(" "));
        }
    }

    @Nested
    @DisplayName("null/默认值处理测试")
    class NullDefaultHandlingTests {

        @Test
        @DisplayName("nullToEmpty null 返回空字符串")
        void testNullToEmptyNull() {
            assertEquals("", StringUtils.nullToEmpty(null));
        }

        @Test
        @DisplayName("nullToEmpty 非null 返回原值")
        void testNullToEmptyNotNull() {
            assertEquals("hello", StringUtils.nullToEmpty("hello"));
        }

        @Test
        @DisplayName("emptyToNull 空白返回 null")
        void testEmptyToNullBlank() {
            assertNull(StringUtils.emptyToNull(""));
            assertNull(StringUtils.emptyToNull("  "));
            assertNull(StringUtils.emptyToNull(null));
        }

        @Test
        @DisplayName("emptyToNull 非空白返回原值")
        void testEmptyToNullNotBlank() {
            assertEquals("hello", StringUtils.emptyToNull("hello"));
        }

        @Test
        @DisplayName("defaultIfBlank 空白返回默认值")
        void testDefaultIfBlankBlank() {
            assertEquals("default", StringUtils.defaultIfBlank(null, "default"));
            assertEquals("default", StringUtils.defaultIfBlank("", "default"));
        }

        @Test
        @DisplayName("defaultIfBlank 非空白返回原值")
        void testDefaultIfBlankNotBlank() {
            assertEquals("hello", StringUtils.defaultIfBlank("hello", "default"));
        }
    }

    @Nested
    @DisplayName("格式验证测试")
    class FormatValidationTests {

        @Test
        @DisplayName("有效邮箱")
        void testValidEmail() {
            assertTrue(StringUtils.isValidEmail("user@example.com"));
            assertTrue(StringUtils.isValidEmail("testuser@domain.org"));
        }

        @Test
        @DisplayName("无效邮箱")
        void testInvalidEmail() {
            assertFalse(StringUtils.isValidEmail("invalid"));
            assertFalse(StringUtils.isValidEmail("@example.com"));
            assertFalse(StringUtils.isValidEmail("user@"));
            assertFalse(StringUtils.isValidEmail(null));
            assertFalse(StringUtils.isValidEmail(""));
        }

        @Test
        @DisplayName("有效手机号")
        void testValidPhone() {
            assertTrue(StringUtils.isValidPhone("13800138000"));
            assertTrue(StringUtils.isValidPhone("18612345678"));
        }

        @Test
        @DisplayName("无效手机号")
        void testInvalidPhone() {
            assertFalse(StringUtils.isValidPhone("12345678901"));
            assertFalse(StringUtils.isValidPhone("1380013800"));
            assertFalse(StringUtils.isValidPhone("23800138000"));
            assertFalse(StringUtils.isValidPhone(null));
        }

        @Test
        @DisplayName("数字判断")
        void testIsNumeric() {
            assertTrue(StringUtils.isNumeric("123"));
            assertTrue(StringUtils.isNumeric("-123"));
            assertTrue(StringUtils.isNumeric("12.34"));
            assertTrue(StringUtils.isNumeric("-12.34"));
            assertFalse(StringUtils.isNumeric("abc"));
            assertFalse(StringUtils.isNumeric(null));
        }

        @Test
        @DisplayName("整数判断")
        void testIsInteger() {
            assertTrue(StringUtils.isInteger("123"));
            assertTrue(StringUtils.isInteger("-123"));
            assertFalse(StringUtils.isInteger("12.34"));
            assertFalse(StringUtils.isInteger("abc"));
        }

        @Test
        @DisplayName("UUID 判断")
        void testIsUuid() {
            assertTrue(StringUtils.isUuid("550e8400-e29b-41d4-a716-446655440000"));
            assertFalse(StringUtils.isUuid("not-a-uuid"));
            assertFalse(StringUtils.isUuid(null));
        }
    }

    @Nested
    @DisplayName("中文字符测试")
    class ChineseCharacterTests {

        @Test
        @DisplayName("包含中文")
        void testContainsChinese() {
            assertTrue(StringUtils.containsChinese("Hello 世界"));
            assertTrue(StringUtils.containsChinese("中文"));
            assertFalse(StringUtils.containsChinese("Hello World"));
            assertFalse(StringUtils.containsChinese(null));
        }

        @Test
        @DisplayName("中文字符计数")
        void testGetChineseCharCount() {
            assertEquals(2, StringUtils.getChineseCharCount("Hello 世界"));
            assertEquals(4, StringUtils.getChineseCharCount("中文测试"));
            assertEquals(0, StringUtils.getChineseCharCount("Hello"));
            assertEquals(0, StringUtils.getChineseCharCount(null));
        }
    }

    @Nested
    @DisplayName("命名转换测试")
    class NamingConversionTests {

        @Test
        @DisplayName("下划线转驼峰")
        void testToCamelCaseFromUnderscore() {
            assertEquals("helloWorld", StringUtils.toCamelCase("hello_world", true));
            assertEquals("helloWorld", StringUtils.toCamelCase("hello_world", false));
        }

        @Test
        @DisplayName("连字符转驼峰")
        void testToCamelCaseFromHyphen() {
            assertEquals("helloWorld", StringUtils.toCamelCase("hello-world", true));
        }

        @Test
        @DisplayName("驼峰转下划线")
        void testToUnderScoreCase() {
            assertEquals("hello_world", StringUtils.toUnderScoreCase("helloWorld"));
            assertEquals("hello", StringUtils.toUnderScoreCase("hello"));
        }

        @Test
        @DisplayName("空白输入转换")
        void testConversionWithBlankInput() {
            assertNull(StringUtils.toCamelCase(null, true));
            assertNull(StringUtils.toUnderScoreCase(null));
        }
    }

    @Nested
    @DisplayName("字符串操作测试")
    class StringManipulationTests {

        @Test
        @DisplayName("重复字符串")
        void testRepeat() {
            assertEquals("abcabcabc", StringUtils.repeat("abc", 3));
            assertEquals("", StringUtils.repeat("abc", 0));
            assertEquals("", StringUtils.repeat("abc", -1));
            assertEquals("", StringUtils.repeat(null, 3));
        }

        @Test
        @DisplayName("截断字符串")
        void testTruncate() {
            assertEquals("Hel...", StringUtils.truncate("Hello World", 6, "..."));
            assertEquals("Hello World", StringUtils.truncate("Hello World", 20, "..."));
            assertEquals("Hel", StringUtils.truncate("Hello World", 3, null));
            assertNull(StringUtils.truncate(null, 10, "..."));
            assertEquals("", StringUtils.truncate("", 10, "..."));
        }
    }
}
