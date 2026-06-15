package com.tailoris.common.util;

import com.tailoris.common.filter.TraceIdFilter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * AuditLogUtils 单元测试 - L-23 修复验证.
 *
 * <p>主要验证：</p>
 * <ul>
 *   <li>各审计方法不抛异常</li>
 *   <li>日志调用通过 LogCaptor 等技术无法直接捕获（依赖外部 appender），
 *       因此重点验证流程完整性</li>
 * </ul>
 *
 * @author Tailor IS Team
 * @since 1.0.0
 */
@DisplayName("AuditLogUtils 审计日志测试 (L-23)")
class AuditLogUtilsTest {

    private static final Logger log = LoggerFactory.getLogger(AuditLogUtilsTest.class);

    @AfterEach
    void cleanup() {
        MDC.clear();
    }

    @Test
    @DisplayName("login 方法应正常执行（不抛异常）")
    void login_NormalCase() {
        AtomicBoolean success = new AtomicBoolean(false);
        try {
            AuditLogUtils.login("user-001", "admin", true, "192.168.1.10");
            AuditLogUtils.login("user-001", "admin", false, "192.168.1.10");
            success.set(true);
        } catch (Exception e) {
            log.error("审计日志调用异常", e);
        }
        assertTrue(success.get());
    }

    @Test
    @DisplayName("logout 方法应正常执行")
    void logout_NormalCase() {
        AuditLogUtils.logout("user-001", "admin");
        assertTrue(true);
    }

    @Test
    @DisplayName("passwordChange 方法应正常执行")
    void passwordChange_NormalCase() {
        AuditLogUtils.passwordChange("user-001", "admin");
        assertTrue(true);
    }

    @Test
    @DisplayName("moneyChange 各操作类型应正常执行")
    void moneyChange_AllOperations() {
        AuditLogUtils.moneyChange("WITHDRAW", "100.00", "user-001", "ORD2026060300001");
        AuditLogUtils.moneyChange("RECHARGE", "500.00", "user-001", "ORD2026060300002");
        AuditLogUtils.moneyChange("REFUND", "99.00", "user-001", "ORD2026060300003");
        AuditLogUtils.moneyChange("TRANSFER", "1000.00", "user-001", "ORD2026060300004");
        assertTrue(true);
    }

    @Test
    @DisplayName("dataModify 各动作应正常执行")
    void dataModify_AllActions() {
        AuditLogUtils.dataModify("ORDER", "10086", "CREATE", "user-001", "创建订单");
        AuditLogUtils.dataModify("ORDER", "10086", "UPDATE", "user-001", "修改订单");
        AuditLogUtils.dataModify("ORDER", "10086", "DELETE", "user-001", "删除订单");
        assertTrue(true);
    }

    @Test
    @DisplayName("audit 通用方法应正常执行")
    void audit_GenericMethod() {
        AuditLogUtils.audit("CUSTOM_OP", "user-001", "admin", "SUCCESS", "自定义审计");
        assertTrue(true);
    }

    @Test
    @DisplayName("MDC 中存在 traceId 时审计日志应能访问")
    void audit_WithMdcTraceId() {
        MDC.put(TraceIdFilter.MDC_TRACE_ID, "audit-trace-123");
        AuditLogUtils.audit("TEST_OP", "user-001", "admin", "SUCCESS", "测试traceId传递");
        // TraceUtils.currentTraceId() 应能获取到
        String traceId = TraceUtils.currentTraceId();
        assertTrue(traceId.equals("audit-trace-123"));
    }
}
