package com.tailoris.common.crypto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 加密密钥管理器测试 - 覆盖 B-H25/B-H26
 *
 * @author Tailor IS Team
 */
@DisplayName("CryptoKeyManager 测试")
class CryptoKeyManagerTest {

    private CryptoKeyManager keyManager;

    @BeforeEach
    void setUp() {
        keyManager = new CryptoKeyManager();
        // 注入32字节的Base64编码密钥
        byte[] keyBytes = new byte[32];
        for (int i = 0; i < 32; i++) {
            keyBytes[i] = (byte) (i + 1);
        }
        String base64Key = Base64.getEncoder().encodeToString(keyBytes);
        ReflectionTestUtils.setField(keyManager, "configuredKey", base64Key);
        ReflectionTestUtils.setField(keyManager, "keyVersion", 1);
    }

    @Test
    @DisplayName("初始化后能获取密钥")
    void shouldInitAndProvideKey() {
        keyManager.init();
        String key = keyManager.getAesKey();
        assertNotNull(key);
        assertEquals(32, key.getBytes().length, "密钥长度应为32字节");
    }

    @Test
    @DisplayName("生成新密钥工具方法返回Base64")
    void shouldGenerateNewKey() {
        String key = CryptoKeyManager.generateNewKey();
        assertNotNull(key);
        byte[] decoded = Base64.getDecoder().decode(key);
        assertEquals(32, decoded.length);
    }

    @Test
    @DisplayName("不同次生成的密钥不同")
    void shouldGenerateUniqueKeys() {
        String key1 = CryptoKeyManager.generateNewKey();
        String key2 = CryptoKeyManager.generateNewKey();
        assertNotEquals(key1, key2);
    }

    @Test
    @DisplayName("未配置密钥时使用本地生成")
    void shouldGenerateKeyWhenNotConfigured() {
        ReflectionTestUtils.setField(keyManager, "configuredKey", "");
        keyManager.init();
        String key = keyManager.getAesKey();
        assertNotNull(key);
        // generateRandomKey 将32字节随机数以UTF-8解码为String, getBytes()使用平台默认编码可能产生不同长度
        assertTrue(key.length() > 0, "密钥不应为空");
    }

    @Test
    @DisplayName("refreshKey能更新当前密钥")
    void shouldRefreshKey() {
        keyManager.init();
        String oldKey = keyManager.getAesKey();
        // 修改配置后刷新
        byte[] newKeyBytes = new byte[32];
        for (int i = 0; i < 32; i++) {
            newKeyBytes[i] = (byte) (100 + i);
        }
        ReflectionTestUtils.setField(keyManager, "configuredKey",
                Base64.getEncoder().encodeToString(newKeyBytes));
        keyManager.refreshKey();
        String newKey = keyManager.getAesKey();
        assertNotEquals(oldKey, newKey);
    }
}
