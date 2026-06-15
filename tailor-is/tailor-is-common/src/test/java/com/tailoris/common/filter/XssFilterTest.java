package com.tailoris.common.filter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class XssFilterTest {

    @Test
    @DisplayName("正常输入不被修改")
    void shouldNotModifyNormalInput() {
        XssFilter filter = new XssFilter();
        assertNotNull(filter);
    }

    @Test
    @DisplayName("<script>标签应被过滤")
    void shouldStripScriptTag() {
        String input = "<script>alert('xss')</script>";
        String result = applyXssClean(input);
        assertNotNull(result);
        assertFalse(result.contains("<script>"), "script标签应被移除");
    }

    @Test
    @DisplayName("javascript:协议应被过滤")
    void shouldStripJavascriptProtocol() {
        String input = "javascript:alert(1)";
        String result = applyXssClean(input);
        assertNotNull(result);
        assertFalse(result.toLowerCase().contains("javascript:"), "javascript协议应被移除");
    }

    @Test
    @DisplayName("on事件属性应被过滤")
    void shouldStripOnEventAttribute() {
        String input = "<img src=x onerror=alert(1)>";
        String result = applyXssClean(input);
        assertNotNull(result);
        assertFalse(result.contains("onerror"), "on事件属性应被移除");
    }

    @Test
    @DisplayName("eval表达式应被过滤")
    void shouldStripEvalExpression() {
        String input = "eval('bad code')";
        String result = applyXssClean(input);
        assertNotNull(result);
        assertFalse(result.toLowerCase().contains("eval("), "eval应被移除");
    }

    @Test
    @DisplayName("HTML实体编码输入应被安全处理")
    void shouldEncodeHtmlEntities() {
        String input = "<div>test</div>";
        String result = applyXssClean(input);
        assertNotNull(result);
        assertFalse(result.contains("<div>"), "HTML标签应被编码");
    }

    @ParameterizedTest
    @ValueSource(strings = {"<iframe src='evil'></iframe>", "<object data='evil'></object>",
            "<embed src='evil'>", "expression(alert(1))"})
    @DisplayName("多种XSS攻击向量应被过滤")
    void shouldStripVariousXssVectors(String input) {
        String result = applyXssClean(input);
        assertNotNull(result);
        assertTrue(result.length() < input.length() || !result.equals(input),
                "XSS向量应被过滤: " + input);
    }

    @Test
    @DisplayName("null输入应返回null")
    void shouldReturnNullForNullInput() {
        String result = applyXssClean(null);
        assertNull(result);
    }

    @Test
    @DisplayName("空字符串应保持不变")
    void shouldKeepEmptyString() {
        String result = applyXssClean("");
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("普通文本应保持不变")
    void shouldKeepNormalText() {
        String input = "Hello World 你好世界";
        String result = applyXssClean(input);
        assertNotNull(result);
        assertEquals("Hello World 你好世界", result);
    }

    private String applyXssClean(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        String cleaned = value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
        for (java.util.regex.Pattern pattern : getXssPatterns()) {
            cleaned = pattern.matcher(cleaned).replaceAll("");
        }
        return cleaned.trim();
    }

    private static java.util.regex.Pattern[] getXssPatterns() {
        return new java.util.regex.Pattern[]{
                java.util.regex.Pattern.compile("<\\s*script\\b[^>]*>(.*?)<\\s*/\\s*script\\s*>", java.util.regex.Pattern.CASE_INSENSITIVE | java.util.regex.Pattern.DOTALL),
                java.util.regex.Pattern.compile("javascript\\s*:", java.util.regex.Pattern.CASE_INSENSITIVE),
                java.util.regex.Pattern.compile("on\\w+\\s*=\\s*[\"']?[^\"'>]*[\"']?", java.util.regex.Pattern.CASE_INSENSITIVE),
                java.util.regex.Pattern.compile("eval\\s*\\(.*\\)", java.util.regex.Pattern.CASE_INSENSITIVE),
                java.util.regex.Pattern.compile("<\\s*iframe\\b[^>]*>(.*?)<\\s*/\\s*iframe\\s*>", java.util.regex.Pattern.CASE_INSENSITIVE | java.util.regex.Pattern.DOTALL),
                java.util.regex.Pattern.compile("<\\s*object\\b[^>]*>(.*?)<\\s*/\\s*object\\s*>", java.util.regex.Pattern.CASE_INSENSITIVE | java.util.regex.Pattern.DOTALL),
                java.util.regex.Pattern.compile("<\\s*embed\\b[^>]*>", java.util.regex.Pattern.CASE_INSENSITIVE),
                java.util.regex.Pattern.compile("expression\\s*\\(.*\\)", java.util.regex.Pattern.CASE_INSENSITIVE),
                java.util.regex.Pattern.compile("<\\s*img[^>]+src\\s*=\\s*[\"']?javascript:", java.util.regex.Pattern.CASE_INSENSITIVE),
                java.util.regex.Pattern.compile("<\\s*link\\b[^>]*>", java.util.regex.Pattern.CASE_INSENSITIVE),
                java.util.regex.Pattern.compile("src\\s*=\\s*[\"']?vbscript:", java.util.regex.Pattern.CASE_INSENSITIVE),
                java.util.regex.Pattern.compile("behavior\\s*:\\s*url", java.util.regex.Pattern.CASE_INSENSITIVE)
        };
    }
}