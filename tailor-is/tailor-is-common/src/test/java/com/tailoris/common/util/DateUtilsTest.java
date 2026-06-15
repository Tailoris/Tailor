package com.tailoris.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("日期工具类测试")
class DateUtilsTest {

    @Test
    @DisplayName("格式化日期时间 - 默认格式")
    void testFormat() {
        LocalDateTime dateTime = LocalDateTime.of(2024, 1, 15, 14, 30, 45);
        String formatted = DateUtils.format(dateTime);
        assertEquals("2024-01-15 14:30:45", formatted);
    }

    @Test
    @DisplayName("格式化日期 - 仅日期")
    void testFormatDate() {
        LocalDateTime dateTime = LocalDateTime.of(2024, 1, 15, 14, 30, 45);
        String formatted = DateUtils.formatDate(dateTime);
        assertEquals("2024-01-15", formatted);
    }

    @Test
    @DisplayName("解析日期时间字符串")
    void testParse() {
        String dateStr = "2024-01-15 14:30:45";
        LocalDateTime parsed = DateUtils.parse(dateStr);
        assertNotNull(parsed);
        assertEquals(2024, parsed.getYear());
        assertEquals(1, parsed.getMonthValue());
        assertEquals(15, parsed.getDayOfMonth());
        assertEquals(14, parsed.getHour());
        assertEquals(30, parsed.getMinute());
        assertEquals(45, parsed.getSecond());
    }

    @Test
    @DisplayName("格式化和解析互为逆操作")
    void testFormatAndParse() {
        LocalDateTime original = LocalDateTime.of(2024, 6, 20, 10, 15, 30);
        String formatted = DateUtils.format(original);
        LocalDateTime parsed = DateUtils.parse(formatted);
        assertEquals(original, parsed);
    }

    @Test
    @DisplayName("格式化不同日期")
    void testFormatDifferentDates() {
        LocalDateTime newYear = LocalDateTime.of(2024, 1, 1, 0, 0, 0);
        assertEquals("2024-01-01 00:00:00", DateUtils.format(newYear));

        LocalDateTime endOfYear = LocalDateTime.of(2024, 12, 31, 23, 59, 59);
        assertEquals("2024-12-31 23:59:59", DateUtils.format(endOfYear));
    }

    @Test
    @DisplayName("格式化日期 - 不同月份")
    void testFormatDateDifferentMonths() {
        LocalDateTime january = LocalDateTime.of(2024, 1, 15, 10, 0, 0);
        assertEquals("2024-01-15", DateUtils.formatDate(january));

        LocalDateTime december = LocalDateTime.of(2024, 12, 25, 10, 0, 0);
        assertEquals("2024-12-25", DateUtils.formatDate(december));
    }
}
