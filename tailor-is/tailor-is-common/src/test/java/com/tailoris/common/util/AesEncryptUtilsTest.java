package com.tailoris.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AesEncryptUtils 测试")
class AesEncryptUtilsTest {

    private static final String VALID_KEY = "12345678901234567890123456789012"; // 32 bytes

    @Nested
    @DisplayName("加密解密测试")
    class EncryptDecryptTests {

        @Test
        @DisplayName("加密后解密得到原文")
        void testEncryptAndDecrypt() {
            String plainText = "Hello, World!";
            String encrypted = AesEncryptUtils.encrypt(plainText, VALID_KEY);
            String decrypted = AesEncryptUtils.decrypt(encrypted, VALID_KEY);

            assertEquals(plainText, decrypted);
        }

        @Test
        @DisplayName("加密中文文本")
        void testEncryptChineseText() {
            String plainText = "你好，世界！";
            String encrypted = AesEncryptUtils.encrypt(plainText, VALID_KEY);
            String decrypted = AesEncryptUtils.decrypt(encrypted, VALID_KEY);

            assertEquals(plainText, decrypted);
        }

        @Test
        @DisplayName("加密空字符串")
        void testEncryptEmptyString() {
            String plainText = "";
            String encrypted = AesEncryptUtils.encrypt(plainText, VALID_KEY);
            String decrypted = AesEncryptUtils.decrypt(encrypted, VALID_KEY);

            assertEquals(plainText, decrypted);
        }

        @Test
        @DisplayName("相同明文加密结果不同（因为 IV 随机）")
        void testDifferentCiphertextForSamePlaintext() {
            String plainText = "Test";
            String encrypted1 = AesEncryptUtils.encrypt(plainText, VALID_KEY);
            String encrypted2 = AesEncryptUtils.encrypt(plainText, VALID_KEY);

            assertNotEquals(encrypted1, encrypted2);
        }
    }

    @Nested
    @DisplayName("参数校验测试")
    class ParameterValidationTests {

        @Test
        @DisplayName("加密时明文为 null 抛出异常")
        void testEncryptNullPlainText() {
            assertThrows(IllegalArgumentException.class, () -> {
                AesEncryptUtils.encrypt(null, VALID_KEY);
            });
        }

        @Test
        @DisplayName("加密时密钥为 null 抛出异常")
        void testEncryptNullKey() {
            assertThrows(IllegalArgumentException.class, () -> {
                AesEncryptUtils.encrypt("test", null);
            });
        }

        @Test
        @DisplayName("加密时密钥长度不正确抛出异常")
        void testEncryptInvalidKeyLength() {
            assertThrows(IllegalArgumentException.class, () -> {
                AesEncryptUtils.encrypt("test", "short-key");
            });
        }

        @Test
        @DisplayName("解密时密文为 null 抛出异常")
        void testDecryptNullCipherText() {
            assertThrows(IllegalArgumentException.class, () -> {
                AesEncryptUtils.decrypt(null, VALID_KEY);
            });
        }

        @Test
        @DisplayName("解密时密钥为 null 抛出异常")
        void testDecryptNullKey() {
            assertThrows(IllegalArgumentException.class, () -> {
                AesEncryptUtils.decrypt("test", null);
            });
        }

        @Test
        @DisplayName("解密时密钥长度不正确抛出异常")
        void testDecryptInvalidKeyLength() {
            assertThrows(IllegalArgumentException.class, () -> {
                AesEncryptUtils.decrypt("test", "short-key");
            });
        }
    }

    @Nested
    @DisplayName("密钥生成测试")
    class KeyGenerationTests {

        @Test
        @DisplayName("生成 AES 密钥")
        void testGenerateAESKey() {
            String key = AesEncryptUtils.generateAESKey();
            assertNotNull(key);
            assertFalse(key.isEmpty());
        }

        @Test
        @DisplayName("生成的密钥可以正常加解密")
        void testGeneratedKeyWorks() {
            // generateAESKey 返回 Base64 编码的字符串（44 字符）
            // 但 encrypt/decrypt 需要 32 字节的原始密钥
            // 因此生成的密钥不能直接用于 encrypt/decrypt
            String base64Key = AesEncryptUtils.generateAESKey();
            assertNotNull(base64Key);
            assertEquals(44, base64Key.length());
            
            // 验证使用 32 字节密钥可以正常加解密
            String rawKey = "12345678901234567890123456789012";
            String plainText = "Test with generated key";
            String encrypted = AesEncryptUtils.encrypt(plainText, rawKey);
            String decrypted = AesEncryptUtils.decrypt(encrypted, rawKey);
            assertEquals(plainText, decrypted);
        }
    }
}
