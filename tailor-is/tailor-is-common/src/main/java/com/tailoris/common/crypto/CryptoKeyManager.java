package com.tailoris.common.crypto;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 加密密钥管理器 - 修复 B-H25/B-H26
 *
 * <p>统一管理项目内所有加密密钥的获取和轮换：</p>
 * <ul>
 *   <li>支持从环境变量读取</li>
 *   <li>支持从Nacos配置中心读取</li>
 *   <li>支持密钥热更新</li>
 *   <li>支持AES-256密钥（32字节）</li>
 * </ul>
 *
 * <h3>配置优先级</h3>
 * <ol>
 *   <li>环境变量 AES_KEY（生产推荐）</li>
 *   <li>配置属性 tailoris.crypto.aes-key</li>
 *   <li>本地默认值（仅开发环境）</li>
 * </ol>
 *
 * @author Tailor IS Team
 * @since 1.1.0
 */
@Slf4j
@Component
@RefreshScope
public class CryptoKeyManager {

    @Value("${tailoris.crypto.aes-key:}")
    private String configuredKey;

    /** 密钥版本（用于轮换） */
    @Value("${tailoris.crypto.aes-key-version:1}")
    private int keyVersion;

    /** 当前生效的密钥（线程安全） */
    private final AtomicReference<String> currentKey = new AtomicReference<>();

    /** 密钥长度（AES-256） */
    private static final int AES_KEY_LENGTH = 32;

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    @PostConstruct
    public void init() {
        refreshKey();
        log.info("CryptoKeyManager 初始化完成，密钥版本: v{}", keyVersion);
    }

    /**
     * 刷新密钥（支持热更新）.
     *
     * <p>生产/测试环境强制要求配置 AES_KEY，缺失时拒绝启动，
     * 防止不同实例使用不同随机密钥导致加密数据无法互通。</p>
     */
    public void refreshKey() {
        String key = loadKey();
        if (key != null) {
            currentKey.set(key);
            log.info("加密密钥已更新，版本: v{}", keyVersion);
        } else if (isDevProfile()) {
            log.warn("开发环境未配置加密密钥，将使用本地随机生成（每次重启密钥不同）");
            currentKey.set(generateRandomKey());
        } else {
            throw new IllegalStateException(
                    "生产/测试环境必须配置 AES_KEY 环境变量或 tailoris.crypto.aes-key 配置项。" +
                    "当前 active profile: " + activeProfile + "。" +
                    "生成新密钥: java -cp tailor-is-common.jar com.tailoris.common.crypto.CryptoKeyManager generateNewKey"
            );
        }
    }

    private boolean isDevProfile() {
        return activeProfile != null && (activeProfile.contains("dev") || activeProfile.contains("default"));
    }

    /**
     * 获取当前AES密钥
     *
     * @return 32字节的密钥
     */
    public String getAesKey() {
        String key = currentKey.get();
        if (key == null || key.isEmpty()) {
            log.error("AES密钥未配置");
            return null;
        }
        return key;
    }

    /**
     * 加载密钥
     */
    private String loadKey() {
        // 1. 优先从环境变量
        String envKey = System.getenv("AES_KEY");
        if (envKey != null && !envKey.isEmpty()) {
            return normalizeKey(envKey);
        }

        // 2. 从配置属性
        if (configuredKey != null && !configuredKey.isEmpty()) {
            return normalizeKey(configuredKey);
        }

        return null;
    }

    /**
     * 规范化密钥为32字节
     */
    private String normalizeKey(String key) {
        // 如果是Base64编码的，先解码
        try {
            byte[] decoded = Base64.getDecoder().decode(key);
            if (decoded.length == AES_KEY_LENGTH) {
                return new String(decoded, StandardCharsets.UTF_8);
            }
        } catch (IllegalArgumentException ignored) {
            // 不是Base64格式，当作原始字符串处理
        }

        // 截断或补齐到32字节
        byte[] bytes = key.getBytes(StandardCharsets.UTF_8);
        if (bytes.length == AES_KEY_LENGTH) {
            return key;
        } else if (bytes.length > AES_KEY_LENGTH) {
            return new String(bytes, 0, AES_KEY_LENGTH, StandardCharsets.UTF_8);
        } else {
            byte[] padded = new byte[AES_KEY_LENGTH];
            System.arraycopy(bytes, 0, padded, 0, bytes.length);
            // 剩余字节填充0
            return new String(padded, StandardCharsets.UTF_8);
        }
    }

    /**
     * 生成随机密钥
     */
    private String generateRandomKey() {
        byte[] key = new byte[AES_KEY_LENGTH];
        new SecureRandom().nextBytes(key);
        return new String(key, StandardCharsets.UTF_8);
    }

    /**
     * 生成新密钥（用于初始配置）
     */
    public static String generateNewKey() {
        byte[] key = new byte[AES_KEY_LENGTH];
        new SecureRandom().nextBytes(key);
        return Base64.getEncoder().encodeToString(key);
    }
}
