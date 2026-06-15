package com.tailoris.common.metrics;

import com.tailoris.common.auth.AuthenticationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * 业务指标定时刷新器
 *  - 每 5 分钟计算一次 abnormal_login_rate（近 5 分钟失败登录占比）
 *  - 每 1 分钟刷新一次 business_active_users（当前活跃用户数）
 *  - 维护 active_users 的最近 60 个历史快照（约 1 小时窗口），
 *    便于 ActiveUsersDropped 规则检测"相比 1h 前下降 50%"
 *
 * 本调度器与 @EnableScheduling（在 TailorIsUserApplication 中启用）配合使用。
 */
@Component
public class MetricsScheduler {

    private static final Logger log = LoggerFactory.getLogger(MetricsScheduler.class);

    private final BusinessMetrics metrics;
    private final AuthenticationService authService;

    @Value("${app.schedule.active-users-seconds:600}")
    private long activeUsersTtlSeconds;

    /** 活跃用户数历史快照（用于对比 1h 前的活跃用户数） */
    private final Deque<Long> activeUsersHistory = new ArrayDeque<>(120);

    public MetricsScheduler(BusinessMetrics metrics, AuthenticationService authService) {
        this.metrics = metrics;
        this.authService = authService;
    }

    /**
     * 每 1 分钟刷新一次活跃用户数（Gauge）
     * 同时维护历史快照，最多保留 120 个（大约 2h）
     */
    @Scheduled(cron = "${app.schedule.refresh-active-users-cron:*/60 * * * * ?}")
    public void refreshActiveUsers() {
        long active = authService.getActiveUsernames().size();
        long now = System.currentTimeMillis();
        metrics.setActiveUsers(active);

        // 记录快照（每分钟 1 条，保留最多 120 条 ≈ 2h）
        synchronized (activeUsersHistory) {
            activeUsersHistory.addLast(now * 1_000_000L + active);  // 编码时间+活跃数
            while (activeUsersHistory.size() > 120) {
                activeUsersHistory.pollFirst();
            }
        }

        log.debug("[MetricsScheduler] 活跃用户刷新: active_users={}", active);
    }

    /**
     * 每 5 分钟计算一次 abnormal_login_rate
     */
    @Scheduled(cron = "${app.schedule.refresh-abnormal-rate-cron:0 */5 * * * ?}")
    public void refreshAbnormalRate() {
        metrics.refreshAbnormalRateFromWindow(5 * 60 * 1000L);
    }

    /**
     * 获取 1h 前的活跃用户数快照（用于外部监控/健康检查/告警逻辑对比）
     */
    public long getActiveUsersOneHourAgo() {
        // 60 分钟前的快照位置；如果不足就拿最早的一条
        long snapshot;
        synchronized (activeUsersHistory) {
            if (activeUsersHistory.size() < 2) return metrics.getActiveUsers();
            Long[] arr = activeUsersHistory.toArray(new Long[0]);
            int idx = Math.max(0, arr.length - 60);
            snapshot = arr[idx];
        }
        // 解码：编码时 ts*1_000_000L + active
        return snapshot % 1_000_000L;
    }
}
