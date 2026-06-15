package com.tailoris.common.crypto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AES-256-GCM 加密测试 - 覆盖 B-H01/B-H25/B-H26
 *
 * @author Tailor IS Team
 */
@DisplayName("AES-256-GCM 加密测试")
class AesGcmCryptoTest {

    private AesGcmCrypto crypto;

    @BeforeEach
    void setUp() {
        crypto = new AesGcmCrypto();
        // 注入32字节的Base64编码密钥
        byte[] keyBytes = new byte[32];
        for (int i = 0; i < 32; i++) {
            keyBytes[i] = (byte) i;
        }
        String base64Key = Base64.getEncoder().encodeToString(keyBytes);
        ReflectionTestUtils.setField(crypto, "configuredKey", base64Key);
        crypto.init();
    }

    @Test
    @DisplayName("加密后能成功解密")
    void shouldEncryptAndDecrypt() {
        String plain = "110101199001011234";
        String encrypted = crypto.encrypt(plain);
        String decrypted = crypto.decrypt(encrypted);
        assertEquals(plain, decrypted);
    }

    @Test
    @DisplayName("相同明文每次加密结果不同（IV随机）")
    void shouldGenerateDifferentCiphertextEachTime() {
        String plain = "test data";
        String encrypted1 = crypto.encrypt(plain);
        String encrypted2 = crypto.encrypt(plain);
        assertNotEquals(encrypted1, encrypted2, "应使用不同IV产生不同密文");
        assertEquals(plain, crypto.decrypt(encrypted1));
        assertEquals(plain, crypto.decrypt(encrypted2));
    }

    @Test
    @DisplayName("null明文加密返回null")
    void shouldReturnNullForNullPlain() {
        assertNull(crypto.encrypt(null));
    }

    @Test
    @DisplayName("null密文解密返回null")
    void shouldReturnNullForNullCipher() {
        assertNull(crypto.decrypt(null));
    }

    @Test
    @DisplayName("空字符串密文解密返回null")
    void shouldReturnNullForEmptyCipher() {
        assertNull(crypto.decrypt(""));
    }

    @Test
    @DisplayName("无效密文应抛异常")
    void shouldThrowOnInvalidCipher() {
        assertThrows(RuntimeException.class, () -> crypto.decrypt("not-valid-base64-!@#"));
    }

    @Test
    @DisplayName("加密长文本")
    void shouldHandleLongText() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            sb.append("这是一段测试文本，包含中文和English混合内容");
        }
        String plain = sb.toString();
        String encrypted = crypto.encrypt(plain);
        String decrypted = crypto.decrypt(encrypted);
        assertEquals(plain, decrypted);
    }

    @Test
    @DisplayName("包含特殊字符的文本")
    void shouldHandleSpecialChars() {
        String plain = "!@#$%^&*()_+-=[]{}|;':\",./<>?`~\\n\\t";
        String encrypted = crypto.encrypt(plain);
        String decrypted = crypto.decrypt(encrypted);
        assertEquals(plain, decrypted);
    }

    @Test
    @DisplayName("未配置密钥时初始化应失败")
    void shouldFailWithoutKey() {
        AesGcmCrypto emptyCrypto = new AesGcmCrypto();
        ReflectionTestUtils.setField(emptyCrypto, "configuredKey", "");
        assertThrows(IllegalStateException.class, emptyCrypto::init);
    }
}
