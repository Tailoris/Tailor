package com.tailoris.common.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TwoFactorAuthService 单元测试
 */
@DisplayName("TwoFactorAuthService 单元测试")
class TwoFactorAuthServiceTest {

    private TwoFactorAuthService authService;

    @BeforeEach
    void setUp() {
        authService = new TwoFactorAuthService();
    }

    @Test
    @DisplayName("生成密钥应返回非空Base64字符串")
    void generateSecret_shouldReturnNonEmptyBase64() {
        String secret = authService.generateSecret();

        assertThat(secret).isNotNull().isNotEmpty();
        assertThat(secret).matches("^[A-Za-z0-9+/]+=*$");
    }

    @Test
    @DisplayName("每次生成的密钥应不同")
    void generateSecret_shouldBeUnique() {
        String secret1 = authService.generateSecret();
        String secret2 = authService.generateSecret();

        assertThat(secret1).isNotEqualTo(secret2);
    }

    @Test
    @DisplayName("绑定用户密钥后应能获取")
    void bindAndGetUserSecret_shouldWork() {
        String userId = "user123";
        String secret = "test-secret";

        authService.bindUserSecret(userId, secret);
        String retrieved = authService.getUserSecret(userId);

        assertThat(retrieved).isEqualTo(secret);
    }

    @Test
    @DisplayName("未绑定用户应返回null")
    void getUserSecret_notBound_shouldReturnNull() {
        String secret = authService.getUserSecret("nonexistent");

        assertThat(secret).isNull();
    }

    @Test
    @DisplayName("检查用户是否启用两步验证-已启用")
    void isEnabled_enabled_shouldReturnTrue() {
        String userId = "user123";
        authService.bindUserSecret(userId, "secret");

        boolean enabled = authService.isEnabled(userId);

        assertThat(enabled).isTrue();
    }

    @Test
    @DisplayName("检查用户是否启用两步验证-未启用")
    void isEnabled_notEnabled_shouldReturnFalse() {
        boolean enabled = authService.isEnabled("nonexistent");

        assertThat(enabled).isFalse();
    }

    @Test
    @DisplayName("禁用用户两步验证后应不再启用")
    void disable_shouldRemoveUserSecret() {
        String userId = "user123";
        authService.bindUserSecret(userId, "secret");
        
        authService.disable(userId);

        assertThat(authService.isEnabled(userId)).isFalse();
        assertThat(authService.getUserSecret(userId)).isNull();
    }

    @Test
    @DisplayName("生成二维码URL应包含正确格式")
    void getQrCodeUrl_shouldReturnCorrectFormat() {
        String secret = authService.generateSecret();
        
        String url = authService.getQrCodeUrl("user@example.com", "TailorIS", secret);

        assertThat(url).startsWith("otpauth://totp/");
        assertThat(url).contains("TailorIS");
        assertThat(url).contains("user%40example.com");
        assertThat(url).contains("algorithm=SHA1");
        assertThat(url).contains("digits=6");
        assertThat(url).contains("period=30");
    }

    @Test
    @DisplayName("生成验证码应返回6位数字")
    void generateCode_shouldReturn6Digits() {
        String secret = authService.generateSecret();
        long timeWindow = authService.getCurrentTimeWindow();

        String code = authService.generateCode(secret, timeWindow);

        assertThat(code).hasSize(6);
        assertThat(code).matches("^\\d{6}$");
    }

    @Test
    @DisplayName("相同密钥和时间窗口应生成相同验证码")
    void generateCode_sameInput_shouldBeDeterministic() {
        String secret = authService.generateSecret();
        long timeWindow = 12345678L;

        String code1 = authService.generateCode(secret, timeWindow);
        String code2 = authService.generateCode(secret, timeWindow);

        assertThat(code1).isEqualTo(code2);
    }

    @Test
    @DisplayName("不同时间窗口应生成不同验证码")
    void generateCode_differentTimeWindow_shouldBeDifferent() {
        String secret = authService.generateSecret();

        String code1 = authService.generateCode(secret, 1000L);
        String code2 = authService.generateCode(secret, 1001L);

        assertThat(code1).isNotEqualTo(code2);
    }

    @Test
    @DisplayName("验证正确验证码应通过")
    void verifyCode_correctCode_shouldPass() {
        String secret = authService.generateSecret();
        long timeWindow = authService.getCurrentTimeWindow();
        String correctCode = authService.generateCode(secret, timeWindow);

        boolean result = authService.verifyCode(secret, correctCode);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("验证错误验证码应失败")
    void verifyCode_incorrectCode_shouldFail() {
        String secret = authService.generateSecret();

        boolean result = authService.verifyCode(secret, "000000");

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("验证null验证码应失败")
    void verifyCode_nullCode_shouldFail() {
        String secret = authService.generateSecret();

        boolean result = authService.verifyCode(secret, null);

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("验证非6位验证码应失败")
    void verifyCode_wrongLength_shouldFail() {
        String secret = authService.generateSecret();

        assertThat(authService.verifyCode(secret, "12345")).isFalse();
        assertThat(authService.verifyCode(secret, "1234567")).isFalse();
        assertThat(authService.verifyCode(secret, "")).isFalse();
    }

    @Test
    @DisplayName("验证前后1个时间窗口的验证码应通过（时钟偏差容忍）")
    void verifyCode_withinWindow_shouldPass() {
        String secret = authService.generateSecret();
        long currentTimeWindow = authService.getCurrentTimeWindow();
        
        String previousCode = authService.generateCode(secret, currentTimeWindow - 1);
        String nextCode = authService.generateCode(secret, currentTimeWindow + 1);

        assertThat(authService.verifyCode(secret, previousCode)).isTrue();
        assertThat(authService.verifyCode(secret, nextCode)).isTrue();
    }

    @Test
    @DisplayName("获取当前时间窗口应返回正数")
    void getCurrentTimeWindow_shouldReturnPositive() {
        long window = authService.getCurrentTimeWindow();

        assertThat(window).isPositive();
    }
}
