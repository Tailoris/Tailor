package com.tailoris.product.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SensitiveWordFilter 敏感词过滤器测试")
class SensitiveWordFilterTest {

    private final SensitiveWordFilter filter = new SensitiveWordFilter();

    @Test
    @DisplayName("空文本过滤")
    void testFilter_EmptyText() {
        SensitiveWordFilter.FilterResult result = filter.filter("");
        assertNotNull(result);
        assertEquals("", result.getFiltered());
        assertEquals(0, result.getHitCount());
        assertFalse(result.hasSensitive());
    }

    @Test
    @DisplayName("null文本过滤")
    void testFilter_NullText() {
        SensitiveWordFilter.FilterResult result = filter.filter(null);
        assertNotNull(result);
        assertNull(result.getFiltered());
        assertEquals(0, result.getHitCount());
        assertFalse(result.hasSensitive());
    }

    @Test
    @DisplayName("正常文本无敏感词")
    void testFilter_NormalText() {
        String text = "这件衣服质量很好，穿着舒适";
        SensitiveWordFilter.FilterResult result = filter.filter(text);
        assertNotNull(result);
        assertEquals(text, result.getFiltered());
        assertEquals(0, result.getHitCount());
        assertFalse(result.hasSensitive());
    }

    @Test
    @DisplayName("包含敏感词-暴力")
    void testFilter_SensitiveWord_Violence() {
        String text = "这个商品太暴力了";
        SensitiveWordFilter.FilterResult result = filter.filter(text);
        assertNotNull(result);
        assertTrue(result.hasSensitive());
        assertTrue(result.getHits().contains("暴力"));
        assertEquals("**", result.getFiltered().substring(result.getFiltered().indexOf("**"), result.getFiltered().indexOf("**") + 2));
    }

    @Test
    @DisplayName("包含敏感词-色情")
    void testFilter_SensitiveWord_Porn() {
        String text = "色情内容不能发布";
        SensitiveWordFilter.FilterResult result = filter.filter(text);
        assertNotNull(result);
        assertTrue(result.hasSensitive());
        assertTrue(result.getHits().contains("色情"));
    }

    @Test
    @DisplayName("包含敏感词-诈骗")
    void testFilter_SensitiveWord_Fraud() {
        String text = "这是诈骗网站";
        SensitiveWordFilter.FilterResult result = filter.filter(text);
        assertNotNull(result);
        assertTrue(result.hasSensitive());
        assertTrue(result.getHits().contains("诈骗"));
    }

    @Test
    @DisplayName("包含广告词-加微信")
    void testFilter_AdWord_Wechat() {
        String text = "加微信获取更多优惠";
        SensitiveWordFilter.FilterResult result = filter.filter(text);
        assertNotNull(result);
        assertTrue(result.hasSensitive());
        assertTrue(result.getHits().contains("加微信"));
    }

    @Test
    @DisplayName("包含手机号")
    void testFilter_PhoneNumber() {
        String text = "联系电话13812345678";
        SensitiveWordFilter.FilterResult result = filter.filter(text);
        assertNotNull(result);
        assertTrue(result.hasSensitive());
        assertTrue(result.getHits().contains("PHONE"));
        assertTrue(result.getFiltered().contains("***"));
    }

    @Test
    @DisplayName("包含QQ号")
    void testFilter_QQNumber() {
        String text = "QQ号123456789";
        SensitiveWordFilter.FilterResult result = filter.filter(text);
        assertNotNull(result);
        assertTrue(result.hasSensitive());
        assertTrue(result.getHits().contains("QQ"));
    }

    @Test
    @DisplayName("包含微信号")
    void testFilter_WechatAccount() {
        String text = "微信号:abc123";
        SensitiveWordFilter.FilterResult result = filter.filter(text);
        assertNotNull(result);
        assertTrue(result.hasSensitive());
        assertTrue(result.getHits().contains("WECHAT"));
    }

    @Test
    @DisplayName("包含URL")
    void testFilter_URL() {
        String text = "访问https://example.com获取更多信息";
        SensitiveWordFilter.FilterResult result = filter.filter(text);
        assertNotNull(result);
        assertTrue(result.hasSensitive());
        assertTrue(result.getHits().contains("URL"));
        assertTrue(result.getFiltered().contains("[链接]"));
    }

    @Test
    @DisplayName("包含邮箱")
    void testFilter_Email() {
        String text = "联系邮箱test@example.com";
        SensitiveWordFilter.FilterResult result = filter.filter(text);
        assertNotNull(result);
        assertTrue(result.hasSensitive());
        assertTrue(result.getHits().contains("EMAIL"));
    }

    @Test
    @DisplayName("多个敏感词")
    void testFilter_MultipleSensitiveWords() {
        String text = "暴力色情内容";
        SensitiveWordFilter.FilterResult result = filter.filter(text);
        assertNotNull(result);
        assertTrue(result.hasSensitive());
        assertTrue(result.getHitCount() >= 2);
        assertTrue(result.getHits().contains("暴力"));
        assertTrue(result.getHits().contains("色情"));
    }

    @Test
    @DisplayName("敏感词替换为星号")
    void testFilter_SensitiveWordReplaced() {
        String text = "这个很暴力";
        SensitiveWordFilter.FilterResult result = filter.filter(text);
        assertNotNull(result);
        assertTrue(result.getFiltered().contains("**"));
        assertFalse(result.getFiltered().contains("暴力"));
    }

    @Test
    @DisplayName("大小写不敏感")
    void testFilter_CaseInsensitive() {
        String text = "加微信WECHAT";
        SensitiveWordFilter.FilterResult result = filter.filter(text);
        assertNotNull(result);
        assertTrue(result.hasSensitive());
    }

    @Test
    @DisplayName("空格会被去除")
    void testFilter_SpaceRemoved() {
        String text = "加 微 信";
        SensitiveWordFilter.FilterResult result = filter.filter(text);
        assertNotNull(result);
        assertTrue(result.hasSensitive());
    }
}
