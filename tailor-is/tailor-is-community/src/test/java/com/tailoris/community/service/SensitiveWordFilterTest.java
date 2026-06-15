package com.tailoris.community.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 敏感词过滤器单元测试（无 Spring 上下文）
 */
@DisplayName("SensitiveWordFilter 单元测试")
class SensitiveWordFilterTest {

    @Test
    @DisplayName("空字符串不包含敏感词")
    void testEmptyText() {
        SensitiveWordFilter filter = new SensitiveWordFilter(null);
        assertFalse(filter.containsSensitive(""));
        assertFalse(filter.containsSensitive(null));
    }

    @Test
    @DisplayName("动态添加敏感词后能识别")
    void testAddAndDetect() {
        SensitiveWordFilter filter = new SensitiveWordFilter(null);
        filter.addWord("违规词");
        filter.addWord("敏感");
        assertTrue(filter.containsSensitive("这是一段违规词的测试"));
        assertTrue(filter.containsSensitive("敏感内容"));
        assertFalse(filter.containsSensitive("正常内容"));
    }

    @Test
    @DisplayName("replace 替换敏感词为 ***")
    void testReplace() {
        SensitiveWordFilter filter = new SensitiveWordFilter(null);
        filter.addWord("违规");
        String result = filter.replace("这里有违规内容");
        assertEquals("这里有***内容", result);
    }

    @Test
    @DisplayName("replace 多个敏感词全部替换")
    void testReplaceMultiple() {
        SensitiveWordFilter filter = new SensitiveWordFilter(null);
        filter.addWord("AB");
        filter.addWord("CD");
        String result = filter.replace("ABCD");
        assertEquals("******", result);
    }

    @Test
    @DisplayName("不存在的敏感词应返回原文")
    void testReplaceNotMatched() {
        SensitiveWordFilter filter = new SensitiveWordFilter(null);
        filter.addWord("XYZ");
        String result = filter.replace("正常内容ABC");
        assertEquals("正常内容ABC", result);
    }
}
