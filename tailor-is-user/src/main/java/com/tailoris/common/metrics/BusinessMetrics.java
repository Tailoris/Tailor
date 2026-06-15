package com.tailoris.common.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicLong;

/**
 * ============================================================
 *  Tailor IS 业务自定义 Metrics
 *  暴露端点: /actuator/prometheus
 *  采集:     Prometheus job_name=tailor-is-microservices
 * ============================================================
 *
 *  本类集中定义所有业务相关的 Prometheus metrics，
 *  便于运维侧编写统一告警规则（见 deploy/alerts.yml）。
 *
 *  === 指标列表 ===
 *  1) admin_login_count           Counter  管理员登录总次数
 *  2) failed_login_attempts       Counter  登录失败总次数（含管理员+普通用户）
 *  3) total_login_attempts        Counter  登录尝试总次数（成功 + 失败）
 *  4) password_reset_requests     Counter  密码重置请求数
 *  5) business_active_users       Gauge    当前活跃用户数
 *  6) abnormal_login_rate         Gauge    近 N 分钟失败登录百分比（整数 0-100）
 *
 *  === 设计原则 ===
 *  - 指标标签保持低基数：只保留 service / env / role / reason 等聚合维度
 *  - username / client_ip 这类高基数信息写入日志，不放入 Prometheus 标签
 *  - Gauge 使用 AtomicLong，便于原子更新且无并发问题
 *
 *  === 告警规则关联 ===
 *  见 deploy/alerts.yml：
 *    - FailedLoginSpike          （5 分钟窗口内失败登录速率 > 10/s）
 *    - HighFailedLoginRate       （失败登录占比 > 20%）
 *    - AbnormalLoginRateCritical （失败登录占比 > 50%）
 *    - AdminLoginActivity        （10 分钟内管理员登录 > 5 次）
 *    - ActiveUsersDropped        （活跃用户相比 1h 前下降 50%）
 *    - PasswordResetSurge        （密码重置请求激增）
 *    - NoLoginActivity           （1h 内无任何登录事件）
 * ============================================================
 */
@Component
public class BusinessMetrics {

    private static final Logger log = LoggerFactory.getLogger(BusinessMetrics.class);

    // 使用 @Lazy 字段注入，避免与 prometheusMeterRegistry 形成循环依赖
    // （prometheusMeterRegistry 初始化时会扫描所有 Meter 注册为 MeterBinder）
    @org.springframework.beans.factory.annotation.Autowired
    @org.springframework.context.annotation.Lazy
    private MeterRegistry registry;

    // ---------- Counters ----------
    private Counter adminLoginCounter;
    private Counter failedLoginCounter;
    private Counter totalLoginCounter;
    private Counter passwordResetCounter;

    // ---------- Gauges（AtomicLong 便于原子更新） ----------
    private final AtomicLong activeUsersGauge = new AtomicLong(0);
    private final AtomicLong abnormalLoginRateGauge = new AtomicLong(0);

    // ---------- 滑动窗口：近 5 分钟登录事件（用于 abnormal_login_rate） ----------
    // 使用 ConcurrentLinkedDeque 存 <时间戳, 是否失败>，由后台 MetricsScheduler 定期清理 & 计算
    private final ConcurrentLinkedDeque<LoginEvent> loginEventWindow = new ConcurrentLinkedDeque<>();

    @jakarta.annotation.PostConstruct
    public void init() {
        // 延迟初始化：确保 prometheusMeterRegistry 已构建完毕后再注册业务指标
        if (registry == null) {
            log.warn("[BusinessMetrics] registry 为 null，跳过初始化（可能在测试环境）");
            return;
        }
        bindTo(registry);
        log.info("[BusinessMetrics] 已初始化 6 个业务指标（4 Counter + 2 Gauge），端点: /actuator/prometheus");
    }

    public void bindTo(MeterRegistry registry) {
        // 1) 管理员登录次数
        adminLoginCounter = Counter.builder("admin_login_count")
                .description("管理员登录总次数")
                .tags(Tags.of("service", "tailor-is-user", "env", "production", "role", "admin"))
                .register(registry);

        // 2) 登录失败次数（按 role 拆分，便于定位管理员 vs 普通用户）
        failedLoginCounter = Counter.builder("failed_login_attempts")
                .description("登录失败总次数（按角色区分）")
                .tags(Tags.of("service", "tailor-is-user", "env", "production", "role", "all"))
                .register(registry);

        // 3) 总登录次数（用于计算失败率）
        totalLoginCounter = Counter.builder("total_login_attempts")
                .description("登录尝试总次数（成功 + 失败）")
                .tags(Tags.of("service", "tailor-is-user", "env", "production"))
                .register(registry);

        // 4) 密码重置请求
        passwordResetCounter = Counter.builder("password_reset_requests")
                .description("密码重置请求数")
                .tags(Tags.of("service", "tailor-is-user", "env", "production", "channel", "email"))
                .register(registry);

        // 5) 活跃用户数（Gauge，由 MetricsScheduler 定时刷新）
        Gauge.builder("business_active_users", activeUsersGauge, AtomicLong::doubleValue)
                .description("当前活跃用户数（近 TTL 内有登录/心跳）")
                .tags(Tags.of("service", "tailor-is-user", "env", "production"))
                .register(registry);

        // 6) 异常登录比例（Gauge，由 MetricsScheduler 定时计算）
        Gauge.builder("abnormal_login_rate", abnormalLoginRateGauge, AtomicLong::doubleValue)
                .description("近 5 分钟内失败登录占总登录的百分比（整数 0-100）")
                .tags(Tags.of("service", "tailor-is-user", "env", "production"))
                .register(registry);
    }

    // ============================================================
    //  业务侧调用入口（在登录/认证组件中调用）
    // ============================================================

    /**
     * 管理员登录成功时调用
     * @param username  用户名（仅用于日志，不放入 Prometheus 标签）
     * @param ip        客户端 IP（仅用于日志，不放入 Prometheus 标签）
     */
    public void recordAdminLogin(String username, String ip) {
        adminLoginCounter.increment();
        totalLoginCounter.increment();
        loginEventWindow.add(new LoginEvent(System.currentTimeMillis(), false));
        log.info("[Metrics] 管理员登录成功 user={} ip={} admin_login_count={}",
                username, ip, (long) adminLoginCounter.count());
    }

    /**
     * 普通用户登录成功时调用（仅累计 total_login_attempts）
     */
    public void recordUserLogin(String username, String ip, String role) {
        totalLoginCounter.increment();
        loginEventWindow.add(new LoginEvent(System.currentTimeMillis(), false));
        log.info("[Metrics] 用户登录成功 user={} role={} ip={} total_login_count={}",
                username, role, ip, (long) totalLoginCounter.count());
    }

    /**
     * 登录失败时调用（普通用户 / 管理员统一走这里）
     * @param role    user|admin|unknown - 用于日志，不放入高基数标签
     * @param reason  wrong_password|locked|not_found|unknown
     * @param username 仅用于日志
     * @param ip       仅用于日志
     */
    public void recordFailedLogin(String role, String username, String reason, String ip) {
        failedLoginCounter.increment();
        totalLoginCounter.increment();
        loginEventWindow.add(new LoginEvent(System.currentTimeMillis(), true));
        // 同时按 reason 标签区分，便于分析是密码错还是账号被锁（reason 是低基数）
        Counter.builder("failed_login_attempts")
                .tags(Tags.of(
                        "service", "tailor-is-user",
                        "env", "production",
                        "role", safeRole(role),
                        "reason", safeReason(reason)
                ))
                .register(registry)
                .increment();
        log.warn("[Metrics] 登录失败 user={} role={} reason={} ip={} failed_login_attempts={}",
                username, role, reason, ip, (long) failedLoginCounter.count());
    }

    /**
     * 密码重置请求调用
     */
    public void recordPasswordReset(String channel) {
        passwordResetCounter.increment();
        log.info("[Metrics] 密码重置请求 channel={} password_reset_requests={}",
                channel, (long) passwordResetCounter.count());
    }

    // ============================================================
    //  Gauge 刷新入口（由 MetricsScheduler 调用）
    // ============================================================

    /** 由活跃用户追踪器定时调用，刷新 business_active_users */
    public void setActiveUsers(long count) {
        long old = activeUsersGauge.getAndSet(count);
        if (old != count) {
            log.info("[Metrics] 活跃用户数刷新 {} -> {}", old, count);
        }
    }

    /** 由调度器调用：计算近 windowMs 内登录失败比例，并刷新 abnormal_login_rate Gauge */
    public void refreshAbnormalRateFromWindow(long windowMs) {
        long cutoff = System.currentTimeMillis() - windowMs;
        long total = 0;
        long failed = 0;
        // 遍历 deque，丢弃过期事件，统计有效事件
        while (!loginEventWindow.isEmpty()) {
            LoginEvent e = loginEventWindow.peek();
            if (e == null || e.ts < cutoff) {
                loginEventWindow.poll();
                continue;
            }
            break;
        }
        for (LoginEvent e : loginEventWindow) {
            if (e.ts >= cutoff) {
                total++;
                if (e.failed) failed++;
            }
        }
        long rate = total > 0 ? (failed * 100) / total : 0;
        abnormalLoginRateGauge.set(rate);
        log.info("[Metrics] abnormal_login_rate 刷新: window={}ms total={} failed={} rate={}%",
                windowMs, total, failed, rate);
    }

    // ============================================================
    //  只读 getter（便于单元测试 / 健康检查读取）
    // ============================================================

    public double getAdminLoginCount() { return adminLoginCounter == null ? 0 : adminLoginCounter.count(); }
    public double getFailedLoginAttempts() { return failedLoginCounter == null ? 0 : failedLoginCounter.count(); }
    public double getTotalLoginAttempts() { return totalLoginCounter == null ? 0 : totalLoginCounter.count(); }
    public double getPasswordResetRequests() { return passwordResetCounter == null ? 0 : passwordResetCounter.count(); }
    public long getActiveUsers() { return activeUsersGauge.get(); }
    public long getAbnormalLoginRate() { return abnormalLoginRateGauge.get(); }

    // ============================================================
    //  私有辅助
    // ============================================================

    private String safeRole(String role) {
        if (role == null) return "unknown";
        String r = role.toLowerCase();
        if (r.equals("admin") || r.equals("user") || r.equals("merchant") || r.equals("unknown")) return r;
        return "other";
    }

    private String safeReason(String reason) {
        if (reason == null) return "unknown";
        String r = reason.toLowerCase();
        if (r.equals("wrong_password") || r.equals("locked") || r.equals("not_found") || r.equals("unknown")) return r;
        return "other";
    }

    /** 简单的登录事件 POJO */
    static class LoginEvent {
        final long ts;
        final boolean failed;
        LoginEvent(long ts, boolean failed) {
            this.ts = ts;
            this.failed = failed;
        }
    }
}
