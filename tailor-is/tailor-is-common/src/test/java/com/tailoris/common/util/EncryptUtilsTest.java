package com.tailoris.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("加密工具类测试")
class EncryptUtilsTest {

    private static final String TEST_KEY = "12345678901234567890123456789012";

    @Test
    @DisplayName("AES加密解密 - 正常流程")
    void testEncryptDecrypt() {
        String plainText = "Hello, World!";
        String encrypted = EncryptUtils.encrypt(plainText, TEST_KEY);
        assertNotNull(encrypted);
        assertNotEquals(plainText, encrypted);

        String decrypted = EncryptUtils.decrypt(encrypted, TEST_KEY);
        assertEquals(plainText, decrypted);
    }

    @Test
    @DisplayName("AES加密 - 空文本")
    void testEncryptEmptyText() {
        String plainText = "";
        String encrypted = EncryptUtils.encrypt(plainText, TEST_KEY);
        assertNotNull(encrypted);

        String decrypted = EncryptUtils.decrypt(encrypted, TEST_KEY);
        assertEquals(plainText, decrypted);
    }

    @Test
    @DisplayName("AES加密 - 中文")
    void testEncryptChinese() {
        String plainText = "你好世界";
        String encrypted = EncryptUtils.encrypt(plainText, TEST_KEY);
        assertNotNull(encrypted);

        String decrypted = EncryptUtils.decrypt(encrypted, TEST_KEY);
        assertEquals(plainText, decrypted);
    }

    @Test
    @DisplayName("AES加密 - 长文本")
    void testEncryptLongText() {
        String plainText = "a".repeat(1000);
        String encrypted = EncryptUtils.encrypt(plainText, TEST_KEY);
        assertNotNull(encrypted);

        String decrypted = EncryptUtils.decrypt(encrypted, TEST_KEY);
        assertEquals(plainText, decrypted);
    }

    @Test
    @DisplayName("AES加密 - 特殊字符")
    void testEncryptSpecialChars() {
        String plainText = "!@#$%^&*()_+-=[]{}|;':\",./<>?";
        String encrypted = EncryptUtils.encrypt(plainText, TEST_KEY);
        assertNotNull(encrypted);

        String decrypted = EncryptUtils.decrypt(encrypted, TEST_KEY);
        assertEquals(plainText, decrypted);
    }

    @Test
    @DisplayName("AES加密 - null参数")
    void testEncryptNullParams() {
        assertThrows(IllegalArgumentException.class, () -> {
            EncryptUtils.encrypt(null, TEST_KEY);
        });
        assertThrows(IllegalArgumentException.class, () -> {
            EncryptUtils.encrypt("test", null);
        });
    }

    @Test
    @DisplayName("AES解密 - null参数")
    void testDecryptNullParams() {
        assertThrows(IllegalArgumentException.class, () -> {
            EncryptUtils.decrypt(null, TEST_KEY);
        });
        assertThrows(IllegalArgumentException.class, () -> {
            EncryptUtils.decrypt("test", null);
        });
    }

    @Test
    @DisplayName("AES加密 - 密钥长度错误")
    void testEncryptInvalidKeyLength() {
        assertThrows(IllegalArgumentException.class, () -> {
            EncryptUtils.encrypt("test", "short");
        });
    }

    @Test
    @DisplayName("AES解密 - 密钥长度错误")
    void testDecryptInvalidKeyLength() {
        assertThrows(IllegalArgumentException.class, () -> {
            EncryptUtils.decrypt("test", "short");
        });
    }

    @Test
    @DisplayName("生成AES密钥")
    void testGenerateAESKey() {
        String key = EncryptUtils.generateAESKey();
        assertNotNull(key);
        assertTrue(key.length() > 0);
    }

    @Test
    @DisplayName("SHA-256哈希")
    void testSha256() {
        String input = "test";
        String hash = EncryptUtils.sha256(input);
        assertNotNull(hash);
        assertEquals(64, hash.length());

        String hash2 = EncryptUtils.sha256(input);
        assertEquals(hash, hash2);
    }

    @Test
    @DisplayName("SHA-256哈希 - 空字符串")
    void testSha256Empty() {
        String hash = EncryptUtils.sha256("");
        assertNotNull(hash);
        assertEquals(64, hash.length());
    }

    @Test
    @DisplayName("SHA-256哈希 - null")
    void testSha256Null() {
        String hash = EncryptUtils.sha256(null);
        assertNull(hash);
    }

    @Test
    @DisplayName("SHA-256哈希 - 中文")
    void testSha256Chinese() {
        String hash = EncryptUtils.sha256("你好");
        assertNotNull(hash);
        assertEquals(64, hash.length());
    }

    @Test
    @DisplayName("MD5哈希")
    void testMd5() {
        String input = "test";
        String hash = EncryptUtils.md5(input);
        assertNotNull(hash);
        assertEquals(32, hash.length());

        String hash2 = EncryptUtils.md5(input);
        assertEquals(hash, hash2);
    }

    @Test
    @DisplayName("MD5哈希 - 空字符串")
    void testMd5Empty() {
        String hash = EncryptUtils.md5("");
        assertNotNull(hash);
        assertEquals(32, hash.length());
    }

    @Test
    @DisplayName("MD5哈希 - null")
    void testMd5Null() {
        String hash = EncryptUtils.md5(null);
        assertNull(hash);
    }

    @Test
    @DisplayName("密码哈希 - BCrypt")
    void testHashPassword() {
        String password = "password123";
        String hash = EncryptUtils.hashPassword(password);
        assertNotNull(hash);
        assertNotEquals(password, hash);
        assertTrue(hash.startsWith("$2a$"));
    }

    @Test
    @DisplayName("密码验证 - BCrypt")
    void testVerifyPassword() {
        String password = "password123";
        String hash = EncryptUtils.hashPassword(password);

        assertTrue(EncryptUtils.verifyPassword(password, hash));
        assertFalse(EncryptUtils.verifyPassword("wrongpassword", hash));
    }

    @Test
    @DisplayName("相同密码生成不同哈希")
    void testDifferentHashForSamePassword() {
        String password = "password123";
        String hash1 = EncryptUtils.hashPassword(password);
        String hash2 = EncryptUtils.hashPassword(password);

        assertNotEquals(hash1, hash2);
        assertTrue(EncryptUtils.verifyPassword(password, hash1));
        assertTrue(EncryptUtils.verifyPassword(password, hash2));
    }

    @Test
    @DisplayName("加密相同文本生成不同密文")
    void testDifferentCiphertextForSamePlaintext() {
        String plainText = "test";
        String encrypted1 = EncryptUtils.encrypt(plainText, TEST_KEY);
        String encrypted2 = EncryptUtils.encrypt(plainText, TEST_KEY);

        assertNotEquals(encrypted1, encrypted2);
        assertEquals(plainText, EncryptUtils.decrypt(encrypted1, TEST_KEY));
        assertEquals(plainText, EncryptUtils.decrypt(encrypted2, TEST_KEY));
    }
}
