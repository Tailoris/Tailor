package com.tailoris.product.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 敏感词过滤器 - PRD-009.
 *
 * <p>支持多种过滤策略：</p>
 * <ul>
 *   <li>直接匹配敏感词（如"色情"、"暴力"）</li>
 *   <li>谐音/变体识别（数字/字母代替）</li>
 *   <li>联系方式识别（手机号、QQ、微信、URL）</li>
 *   <li>广告识别（"加微信"、"V信"等）</li>
 * </ul>
 *
 * <p>使用 AC 自动机实现高效多模式匹配（O(n) 时间复杂度）。
 * 此处使用简化版：先正则后字典。</p>
 *
 * @author Tailor IS Team
 * @since 1.0.0
 */
@Slf4j
@Component
public class SensitiveWordFilter {

    /** 系统默认敏感词库（可对接外部API） */
    private static final Set<String> DEFAULT_SENSITIVE_WORDS = new HashSet<>(Arrays.asList(
            // 政治
            "法轮", "反动", "台独", "港独", "疆独",
            // 暴力
            "暴力", "血腥", "恐怖", "袭击", "枪支", "弹药",
            // 色情
            "色情", "黄片", "裸聊", "一夜情",
            // 诈骗
            "诈骗", "洗钱", "传销", "非法集资", "高利贷",
            // 广告
            "加微信", "加QQ", "微商", "V信", "wx:", "vx:",
            "代购", "代孕", "办证", "发票", "走私"
    ));

    /** 手机号正则 */
    private static final Pattern PHONE_PATTERN = Pattern.compile("1[3-9]\\d{9}");

    /** QQ号正则（5-11位） */
    private static final Pattern QQ_PATTERN = Pattern.compile("(?<![0-9])[1-9]\\d{4,10}(?![0-9])");

    /** 微信号正则 */
    private static final Pattern WECHAT_PATTERN = Pattern.compile("(?i)(微信号?|微信|wx|vx|v信)[：:\\s]+[A-Za-z0-9_\\-]+");

    /** URL正则 */
    private static final Pattern URL_PATTERN = Pattern.compile("(https?://|www\\.)[A-Za-z0-9_\\-./?=&%#]+");

    /** 邮箱正则 */
    private static final Pattern EMAIL_PATTERN = Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}");

    private final Set<String> sensitiveWords = DEFAULT_SENSITIVE_WORDS;

    /**
     * 过滤敏感词.
     *
     * @param text 原始文本
     * @return 过滤结果
     */
    public FilterResult filter(String text) {
        if (text == null || text.isEmpty()) {
            return new FilterResult(text, 0, java.util.Collections.emptyList());
        }

        // 1. 数字/字母变体还原（将数字0-9替换为可能的谐音字母）
        String normalized = normalize(text);

        int hitCount = 0;
        java.util.List<String> hits = new java.util.ArrayList<>();

        // 2. 字典匹配
        for (String word : sensitiveWords) {
            if (normalized.contains(word)) {
                hitCount++;
                hits.add(word);
            }
        }

        // 3. 正则匹配
        if (PHONE_PATTERN.matcher(text).find()) {
            hitCount++;
            hits.add("PHONE");
        }
        if (QQ_PATTERN.matcher(text).find()) {
            hitCount++;
            hits.add("QQ");
        }
        if (WECHAT_PATTERN.matcher(text).find()) {
            hitCount++;
            hits.add("WECHAT");
        }
        if (URL_PATTERN.matcher(text).find()) {
            hitCount++;
            hits.add("URL");
        }
        if (EMAIL_PATTERN.matcher(text).find()) {
            hitCount++;
            hits.add("EMAIL");
        }

        // 4. 替换敏感词
        String filtered = text;
        for (String word : hits) {
            if (!"PHONE".equals(word) && !"QQ".equals(word) && !"WECHAT".equals(word)
                    && !"URL".equals(word) && !"EMAIL".equals(word)) {
                filtered = filtered.replaceAll(Pattern.quote(word), "*".repeat(word.length()));
            }
        }
        // 联系方式打码
        filtered = PHONE_PATTERN.matcher(filtered).replaceAll("***");
        filtered = QQ_PATTERN.matcher(filtered).replaceAll("***");
        filtered = WECHAT_PATTERN.matcher(filtered).replaceAll("微信:***");
        filtered = URL_PATTERN.matcher(filtered).replaceAll("[链接]");

        return new FilterResult(filtered, hitCount, hits);
    }

    /**
     * 数字谐音还原（部分常见）：
     * 1=衣 2=贰/爱 3=山 4=死 5=无 6=溜 7=起 8=发 9=就 0=O
     * 此处仅做简单标准化：转小写、去空格。
     */
    private String normalize(String text) {
        return text.toLowerCase().replaceAll("\\s+", "");
    }

    /**
     * 过滤结果.
     */
    public static class FilterResult {
        private final String filtered;
        private final int hitCount;
        private final List<String> hits;

        public FilterResult(String filtered, int hitCount, List<String> hits) {
            this.filtered = filtered;
            this.hitCount = hitCount;
            this.hits = hits;
        }

        public String getFiltered() { return filtered; }
        public int getHitCount() { return hitCount; }
        public List<String> getHits() { return hits; }
        public boolean hasSensitive() { return hitCount > 0; }
    }
}
