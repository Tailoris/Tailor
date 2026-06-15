package com.tailoris.common.auth;

import com.tailoris.common.metrics.BusinessMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 密码重置服务（演示版本）
 *  - 仅接入 BusinessMetrics 用于统计密码重置请求
 *  - 不发送真实邮件（邮件发送请使用 TailorIS Resend 邮件服务）
 */
@Service
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);

    private final BusinessMetrics metrics;

    /** username -> 验证码（6 位随机数字，5 分钟有效） */
    private final ConcurrentHashMap<String, ResetCode> codeStore = new ConcurrentHashMap<>();

    public PasswordResetService(BusinessMetrics metrics) {
        this.metrics = metrics;
    }

    /**
     * 请求密码重置验证码
     * 核心：调用 metrics.recordPasswordReset("email") 累计密码重置请求指标
     */
    public String requestReset(String username) {
        // 生成 6 位验证码
        String code = String.format("%06d", ThreadLocalRandom.current().nextInt(0, 1_000_000));
        codeStore.put(username, new ResetCode(code, System.currentTimeMillis() + 5 * 60 * 1000));

        // 指标上报
        metrics.recordPasswordReset("email");

        log.info("[PasswordReset] 验证码已生成 user={} code_prefix={}",
                username, code.substring(0, 2) + "****");
        return code;  // demo 环境：直接返回验证码；生产环境应发送邮件/短信
    }

    /**
     * 校验验证码并重置密码
     */
    public boolean verifyAndReset(String username, String code, String newPassword) {
        ResetCode rc = codeStore.get(username);
        if (rc == null) {
            log.warn("[PasswordReset] 用户无待处理的重置请求 user={}", username);
            return false;
        }
        if (rc.expiresAt < System.currentTimeMillis()) {
            codeStore.remove(username);
            log.warn("[PasswordReset] 验证码已过期 user={}", username);
            return false;
        }
        if (!rc.code.equals(code)) {
            log.warn("[PasswordReset] 验证码错误 user={}", username);
            return false;
        }
        // 校验通过，删除验证码；在生产环境应更新数据库中的密码
        codeStore.remove(username);
        log.info("[PasswordReset] 密码重置成功 user={}", username);
        return true;
    }

    static class ResetCode {
        String code;
        long expiresAt;
        ResetCode(String code, long expiresAt) {
            this.code = code;
            this.expiresAt = expiresAt;
        }
    }
}
