package com.tailoris.community.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * 敏感词过滤器
 * 任务编号: COM-004 配套 - 内容安全
 *
 * <p>使用前缀树（Trie）实现，线程安全。词库从 classpath:sensitive_words.txt 加载，
 * 也可对接 Redis 远程热更新词库。</p>
 */
@Component
public class SensitiveWordFilter {

    private final StringRedisTemplate stringRedisTemplate;
    private volatile TrieNode root = new TrieNode();
    private final String REDIS_KEY = "community:sensitive:words";
    private final String DEFAULT_REPLACE = "***";

    public SensitiveWordFilter(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @PostConstruct
    public void init() {
        try {
            loadFromClasspath();
        } catch (Exception e) {
            // 词库加载失败时使用空树
        }
    }

    private void loadFromClasspath() throws IOException {
        InputStream is = getClass().getClassLoader().getResourceAsStream("sensitive_words.txt");
        if (is == null) {
            return;
        }
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                addWord(line);
            }
        }
    }

    public synchronized void addWord(String word) {
        if (word == null || word.isEmpty()) return;
        TrieNode node = root;
        for (char c : word.toCharArray()) {
            node = node.children.computeIfAbsent(c, k -> new TrieNode());
        }
        node.end = true;
    }

    public boolean containsSensitive(String text) {
        if (text == null || text.isEmpty()) return false;
        return !searchFirst(text).isEmpty();
    }

    public String replace(String text) {
        if (text == null || text.isEmpty()) return text;
        Set<String> matches = searchAll(text);
        if (matches.isEmpty()) return text;
        StringBuilder sb = new StringBuilder(text);
        java.util.List<int[]> positions = new java.util.ArrayList<>();
        for (String m : matches) {
            int idx = 0;
            while ((idx = text.indexOf(m, idx)) != -1) {
                positions.add(new int[]{idx, idx + m.length()});
                idx += m.length();
            }
        }
        positions.sort((a, b) -> b[0] - a[0]);
        for (int[] pos : positions) {
            sb.replace(pos[0], pos[1], DEFAULT_REPLACE);
        }
        return sb.toString();
    }

    private Set<String> searchFirst(String text) {
        Set<String> result = new HashSet<>();
        for (int i = 0; i < text.length(); i++) {
            TrieNode node = root.children.get(text.charAt(i));
            if (node == null) continue;
            for (int j = i + 1; j <= text.length(); j++) {
                if (node.end) {
                    result.add(text.substring(i, j));
                }
                if (j == text.length()) break;
                node = node.children.get(text.charAt(j));
                if (node == null) break;
            }
        }
        return result;
    }

    private Set<String> searchAll(String text) {
        return searchFirst(text);
    }

    /**
     * 从 Redis 热更新词库
     */
    public void refreshFromRedis() {
        try {
            Set<String> words = stringRedisTemplate.opsForSet().members(REDIS_KEY);
            if (words != null) {
                TrieNode newRoot = new TrieNode();
                for (String w : words) {
                    TrieNode cur = newRoot;
                    for (char c : w.toCharArray()) {
                        cur = cur.children.computeIfAbsent(c, k -> new TrieNode());
                    }
                    cur.end = true;
                }
                this.root = newRoot;
            }
        } catch (Exception e) {
            // 静默失败
        }
    }

    private static class TrieNode {
        HashMap<Character, TrieNode> children = new HashMap<>();
        boolean end = false;
    }
}
