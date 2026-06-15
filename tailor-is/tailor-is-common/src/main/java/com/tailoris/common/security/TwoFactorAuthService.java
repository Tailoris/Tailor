package com.tailoris.common.security;

import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Tailor IS TOTP (Time-based One-Time Password) 两步验证实现
 * 兼容 Google Authenticator / Authy / 1Password / Microsoft Authenticator
 *
 * 使用流程:
 *   1. 用户在安全设置中启用两步验证
 *   2. 系统调用 generateSecret() 生成密钥
 *   3. 系统生成二维码 (otpauth://totp/...) 供用户扫描
 *   4. 用户输入6位验证码完成绑定
 *   5. 后续登录需要输入动态6位验证码
 *
 * 算法实现: RFC 6238 (TOTP) + RFC 4226 (HOTP)
 *
 * @author Tailor IS Platform Team
 * @version 1.0
 */
@Component
public class TwoFactorAuthService {

    private static final String HMAC_ALGO = "HmacSHA1";
    private static final int TIME_STEP_SECONDS = 30;
    private static final int CODE_DIGITS = 6;
    private static final int WINDOW_SIZE = 1; // 允许前后1个时间窗口(±30秒)

    // 用户ID -> 密钥 (生产环境建议存储在数据库, 此处为示例)
    private final Map<String, String> userSecrets = new ConcurrentHashMap<>();

    /**
     * 为用户生成一个新的 TOTP 密钥
     * @return Base64 编码的密钥字符串
     */
    public String generateSecret() {
        byte[] secret = new byte[20]; // 160位密钥 (推荐值)
        new java.security.SecureRandom().nextBytes(secret);
        return Base64.getEncoder().encodeToString(secret);
    }

    /**
     * 将生成的密钥与用户绑定
     */
    public void bindUserSecret(String userId, String secret) {
        userSecrets.put(userId, secret);
    }

    /**
     * 获取用户的 TOTP 密钥，若不存在则返回 null
     */
    public String getUserSecret(String userId) {
        return userSecrets.get(userId);
    }

    /**
     * 检查用户是否已启用两步验证
     */
    public boolean isEnabled(String userId) {
        return userSecrets.containsKey(userId);
    }

    /**
     * 生成 Google Authenticator 格式的二维码内容 URL
     *
     * @param accountName 账户名 (通常是用户邮箱)
     * @param issuer      发行者 (如 "TailorIS")
     * @param secret      Base64 编码的密钥
     * @return otpauth:// URL
     */
    public String getQrCodeUrl(String accountName, String issuer, String secret) {
        return String.format("otpauth://totp/%s:%s?secret=%s&issuer=%s&algorithm=SHA1&digits=%d&period=%d",
                urlEncode(issuer), urlEncode(accountName), toBase32(secret),
                urlEncode(issuer), CODE_DIGITS, TIME_STEP_SECONDS);
    }

    /**
     * 验证当前的 TOTP 验证码
     *
     * @param secret Base64 编码的密钥
     * @param code   用户输入的6位数字验证码
     * @return true: 验证通过
     */
    public boolean verifyCode(String secret, String code) {
        if (code == null || code.length() != CODE_DIGITS) {
            return false;
        }
        long currentTimeWindow = getCurrentTimeWindow();
        // 检查当前窗口及前后各1个窗口（允许±30秒时钟偏差）
        for (int offset = -WINDOW_SIZE; offset <= WINDOW_SIZE; offset++) {
            String expectedCode = generateCode(secret, currentTimeWindow + offset);
            if (expectedCode.equals(code)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 根据密钥和时间窗口生成6位验证码
     */
    public String generateCode(String secret, long timeWindow) {
        byte[] key = Base64.getDecoder().decode(secret);
        byte[] timeData = longToBytes(timeWindow);

        try {
            Mac mac = Mac.getInstance(HMAC_ALGO);
            mac.init(new SecretKeySpec(key, HMAC_ALGO));
            byte[] hmac = mac.doFinal(timeData);

            // RFC 4226: HOTP
            int offset = hmac[hmac.length - 1] & 0xF;
            int binary = ((hmac[offset] & 0x7F) << 24)
                    | ((hmac[offset + 1] & 0xFF) << 16)
                    | ((hmac[offset + 2] & 0xFF) << 8)
                    | (hmac[offset + 3] & 0xFF);

            int otp = binary % (int) Math.pow(10, CODE_DIGITS);
            return String.format("%0" + CODE_DIGITS + "d", otp);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("TOTP 算法初始化失败", e);
        }
    }

    /**
     * 获取当前时间窗口（每30秒递增1）
     */
    public long getCurrentTimeWindow() {
        return Instant.now().getEpochSecond() / TIME_STEP_SECONDS;
    }

    /**
     * 禁用用户的两步验证
     */
    public void disable(String userId) {
        userSecrets.remove(userId);
    }

    // ================ 辅助方法 ================

    private static byte[] longToBytes(long value) {
        byte[] result = new byte[8];
        for (int i = 7; i >= 0; i--) {
            result[i] = (byte) (value & 0xFF);
            value >>= 8;
        }
        return result;
    }

    private static String urlEncode(String value) {
        try {
            return java.net.URLEncoder.encode(value, "UTF-8");
        } catch (java.io.UnsupportedEncodingException e) {
            return value;
        }
    }

    /**
     * Base64 转换为 Base32 (Google Authenticator 使用 Base32)
     */
    private static String toBase32(String base64Secret) {
        byte[] bytes = Base64.getDecoder().decode(base64Secret);
        StringBuilder result = new StringBuilder();
        int buffer = 0;
        int bitsLeft = 0;
        String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";

        for (byte b : bytes) {
            buffer = (buffer << 8) | (b & 0xFF);
            bitsLeft += 8;
            while (bitsLeft >= 5) {
                int index = (buffer >>> (bitsLeft - 5)) & 31;
                result.append(alphabet.charAt(index));
                bitsLeft -= 5;
            }
        }
        if (bitsLeft > 0) {
            int index = (buffer << (5 - bitsLeft)) & 31;
            result.append(alphabet.charAt(index));
        }
        // Base32 需要按8字节补齐
        while (result.length() % 8 != 0) {
            result.append('=');
        }
        return result.toString();
    }
}
