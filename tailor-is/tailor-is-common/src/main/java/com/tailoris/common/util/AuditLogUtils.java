package com.tailoris.common.util;

import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 审计日志工具类 - 修复 L-23.
 *
 * <p>🔒 L-23修复: 关键业务操作（登录、修改密码、删除数据、资金变动等）必须记录审计日志。
 *    审计日志独立于业务日志，结构化字段便于合规审计与安全分析。</p>
 *
 * <p>审计日志级别：WARN（确保生产环境也会输出）</p>
 *
 * <p>使用示例：</p>
 * <pre>
 * AuditLogUtils.login(userId, "admin", true, "192.168.1.10");
 * AuditLogUtils.passwordChange(userId, "admin");
 * AuditLogUtils.dataModify("订单", "10086", "DELETE", userId, "管理员删除了订单");
 * </pre>
 *
 * @author Tailor IS Team
 * @since 1.0.0
 */
@Slf4j
public final class AuditLogUtils {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private AuditLogUtils() {
    }

    /**
     * 记录登录审计日志.
     *
     * @param userId   用户ID
     * @param username 用户名
     * @param success  是否成功
     * @param clientIp 客户端IP
     */
    public static void login(String userId, String username, boolean success, String clientIp) {
        audit("LOGIN", userId, username, success ? "SUCCESS" : "FAILURE",
                String.format("ip=%s", clientIp));
    }

    /**
     * 记录登出审计日志.
     */
    public static void logout(String userId, String username) {
        audit("LOGOUT", userId, username, "SUCCESS", "-");
    }

    /**
     * 记录密码修改审计日志.
     */
    public static void passwordChange(String userId, String username) {
        audit("PASSWORD_CHANGE", userId, username, "SUCCESS", "-");
    }

    /**
     * 记录资金变动审计日志.
     *
     * @param operation   操作类型（WITHDRAW/RECHARGE/REFUND/TRANSFER）
     * @param amount      金额
     * @param userId      用户ID
     * @param orderNo     关联单号
     */
    public static void moneyChange(String operation, String amount, String userId, String orderNo) {
        audit("MONEY_" + operation.toUpperCase(), userId, "-", "SUCCESS",
                String.format("amount=%s, orderNo=%s", amount, orderNo));
    }

    /**
     * 记录数据变更审计日志.
     *
     * @param resource   资源类型（订单/商品/用户等）
     * @param resourceId 资源ID
     * @param action     操作动作（CREATE/UPDATE/DELETE）
     * @param userId     操作用户
     * @param detail     操作详情
     */
    public static void dataModify(String resource, String resourceId, String action,
                                   String userId, String detail) {
        audit(action.toUpperCase() + "_" + resource.toUpperCase(), userId, "-", "SUCCESS",
                String.format("resourceId=%s, detail=%s", resourceId, detail));
    }

    /**
     * 通用审计日志方法.
     *
     * <p>日志格式：{@code AUDIT | 时间 | traceId | 操作 | userId | username | 结果 | 详情}</p>
     */
    public static void audit(String operation, String userId, String username, String result, String detail) {
        String traceId = TraceUtils.currentTraceId();
        log.warn("AUDIT | {} | traceId={} | op={} | userId={} | username={} | result={} | detail={}",
                LocalDateTime.now().format(DATE_FORMATTER),
                traceId, operation, userId, username, result, detail);
    }
}
