package com.tailoris.common.crypto;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-256-GCM 加密工具 - 修复 B-H01/B-H25/B-H26
 *
 * <p>AES-256-GCM 提供加密和认证（防篡改）双重保护。
 * 替代旧的AES-CBC易受padding oracle攻击的问题。</p>
 *
 * <h3>关键改进</h3>
 * <ul>
 *   <li>B-H01: 身份证号AES加密存储</li>
 *   <li>B-H25: 密钥从配置中心读取（不再硬编码）</li>
 *   <li>B-H26: 密钥支持KMS或配置中心动态获取</li>
 * </ul>
 *
 * <h3>输出格式</h3>
 * <p>Base64(iv + ciphertext + tag) — IV为12字节（96位）</p>
 *
 * @author Tailor IS Team
 * @since 1.1.0
 */
@Slf4j
@Component
public class AesGcmCrypto {

    /** AES算法 */
    private static final String ALGORITHM = "AES";
    /** GCM转换 */
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    /** GCM Tag长度（位） */
    private static final int GCM_TAG_LENGTH = 128;
    /** IV长度（字节） */
    private static final int IV_LENGTH = 12;
    /** 密钥长度（字节）— AES-256 */
    private static final int KEY_LENGTH = 32;

    @Value("${tailoris.crypto.aes-key:}")
    private String configuredKey;

    private SecretKey secretKey;
    private final SecureRandom secureRandom = new SecureRandom();

    @PostConstruct
    public void init() {
        // 密钥从配置中心读取，不再硬编码
        byte[] keyBytes = loadKey();
        if (keyBytes == null || keyBytes.length != KEY_LENGTH) {
            throw new IllegalStateException(
                    "AES密钥未配置或长度不正确（需要32字节），请检查 tailoris.crypto.aes-key 配置");
        }
        this.secretKey = new SecretKeySpec(keyBytes, ALGORITHM);
        log.info("AES-256-GCM加密器初始化完成");
    }

    /**
     * 加载密钥
     *
     * <p>优先级：</p>
     * <ol>
     *   <li>环境变量 AES_KEY（生产推荐）</li>
     *   <li>配置中心 Nacos 的 tailoris.crypto.aes-key</li>
     *   <li>本地配置文件 application.yml</li>
     * </ol>
     */
    private byte[] loadKey() {
        // 1. 优先从环境变量
        String envKey = System.getenv("AES_KEY");
        if (envKey != null && !envKey.isEmpty()) {
            return envKey.getBytes(StandardCharsets.UTF_8);
        }

        // 2. 从配置属性
        if (configuredKey != null && !configuredKey.isEmpty()) {
            // 支持Base64编码的密钥
            try {
                return Base64.getDecoder().decode(configuredKey);
            } catch (IllegalArgumentException e) {
                return configuredKey.getBytes(StandardCharsets.UTF_8);
            }
        }

        log.error("AES密钥未配置！请设置环境变量 AES_KEY 或配置 tailoris.crypto.aes-key");
        return null;
    }

    /**
     * 加密
     *
     * @param plain 明文
     * @return Base64编码的密文（IV + Ciphertext + Tag）
     */
    public String encrypt(String plain) {
        if (plain == null) {
            return null;
        }
        try {
            byte[] iv = generateIv();
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec);
            byte[] ciphertext = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));

            // 拼接 IV + Ciphertext
            byte[] result = new byte[IV_LENGTH + ciphertext.length];
            System.arraycopy(iv, 0, result, 0, IV_LENGTH);
            System.arraycopy(ciphertext, 0, result, IV_LENGTH, ciphertext.length);

            return Base64.getEncoder().encodeToString(result);
        } catch (Exception e) {
            log.error("AES加密失败", e);
            throw new CryptoException("加密失败", e);
        }
    }

    /**
     * 解密
     *
     * @param encrypted Base64编码的密文
     * @return 明文
     */
    public String decrypt(String encrypted) {
        if (encrypted == null || encrypted.isEmpty()) {
            return null;
        }
        try {
            byte[] data = Base64.getDecoder().decode(encrypted);
            if (data.length < IV_LENGTH + 16) {
                throw new CryptoException("密文长度不正确");
            }

            byte[] iv = new byte[IV_LENGTH];
            System.arraycopy(data, 0, iv, 0, IV_LENGTH);
            byte[] ciphertext = new byte[data.length - IV_LENGTH];
            System.arraycopy(data, IV_LENGTH, ciphertext, 0, ciphertext.length);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec);
            byte[] plain = cipher.doFinal(ciphertext);

            return new String(plain, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("AES解密失败", e);
            throw new CryptoException("解密失败", e);
        }
    }

    /**
     * 生成随机IV
     */
    private byte[] generateIv() {
        byte[] iv = new byte[IV_LENGTH];
        secureRandom.nextBytes(iv);
        return iv;
    }

    /**
     * 加密异常
     */
    public static class CryptoException extends RuntimeException {
        public CryptoException(String message) {
            super(message);
        }
        public CryptoException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
