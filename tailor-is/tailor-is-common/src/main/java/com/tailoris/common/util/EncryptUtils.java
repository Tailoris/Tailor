package com.tailoris.common.util;

import org.springframework.util.Base64Utils;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public final class EncryptUtils {

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/CBC/PKCS5Padding";
    private static final int KEY_LENGTH = 32;
    private static final int IV_LENGTH = 16;

    private static final ThreadLocal<SecureRandom> SECURE_RANDOM = ThreadLocal.withInitial(SecureRandom::new);

    private EncryptUtils() {
    }

    public static String encrypt(String plainText, String key) {
        if (plainText == null || key == null) {
            throw new IllegalArgumentException("Plain text and key must not be null");
        }
        if (key.length() != KEY_LENGTH) {
            throw new IllegalArgumentException("Key must be " + KEY_LENGTH + " bytes");
        }
        try {
            byte[] iv = generateIV();
            SecretKeySpec keySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), ALGORITHM);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            byte[] result = new byte[IV_LENGTH + encrypted.length];
            System.arraycopy(iv, 0, result, 0, IV_LENGTH);
            System.arraycopy(encrypted, 0, result, IV_LENGTH, encrypted.length);

            return Base64Utils.encodeToString(result);
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }

    public static String decrypt(String cipherText, String key) {
        if (cipherText == null || key == null) {
            throw new IllegalArgumentException("Cipher text and key must not be null");
        }
        if (key.length() != KEY_LENGTH) {
            throw new IllegalArgumentException("Key must be " + KEY_LENGTH + " bytes");
        }
        try {
            byte[] decoded = Base64Utils.decodeFromString(cipherText);
            if (decoded.length < IV_LENGTH) {
                throw new IllegalArgumentException("Invalid cipher text");
            }

            byte[] iv = new byte[IV_LENGTH];
            byte[] encrypted = new byte[decoded.length - IV_LENGTH];
            System.arraycopy(decoded, 0, iv, 0, IV_LENGTH);
            System.arraycopy(decoded, IV_LENGTH, encrypted, 0, encrypted.length);

            SecretKeySpec keySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), ALGORITHM);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
            byte[] decrypted = cipher.doFinal(encrypted);

            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Decryption failed", e);
        }
    }

    public static String generateAESKey() {
        byte[] key = new byte[KEY_LENGTH];
        SECURE_RANDOM.get().nextBytes(key);
        return Base64Utils.encodeToString(key);
    }

    public static String sha256(String input) {
        if (input == null) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 hashing failed", e);
        }
    }

    /**
     * MD5 哈希（不推荐用于安全敏感场景）。
     *
     * <p>M-004 修复: MD5 已不适合用于密码哈希，请使用 {@link #hashPassword(String)} 代替。
     * 此方法仅保留用于非安全用途（如文件校验、数据指纹等）。</p>
     *
     * @deprecated 密码哈希请使用 {@link #hashPassword(String)} (BCrypt)，
     *             非密码哈希请使用 {@link #sha256(String)}。
     */
    @Deprecated
    public static String md5(String input) {
        if (input == null) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new RuntimeException("MD5 hashing failed", e);
        }
    }

    private static final BCryptPasswordEncoder BCRYPT_ENCODER = new BCryptPasswordEncoder(12);

    /**
     * 使用 BCrypt 对密码进行哈希（M-004 推荐方法）。
     *
     * @param rawPassword 明文密码
     * @return BCrypt 哈希值
     */
    public static String hashPassword(String rawPassword) {
        return BCRYPT_ENCODER.encode(rawPassword);
    }

    /**
     * 验证密码与 BCrypt 哈希是否匹配。
     *
     * @param rawPassword     明文密码
     * @param encodedPassword BCrypt 哈希值
     * @return 是否匹配
     */
    public static boolean verifyPassword(String rawPassword, String encodedPassword) {
        return BCRYPT_ENCODER.matches(rawPassword, encodedPassword);
    }

    private static byte[] generateIV() {
        byte[] iv = new byte[IV_LENGTH];
        SECURE_RANDOM.get().nextBytes(iv);
        return iv;
    }
}
