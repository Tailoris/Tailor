package com.tailoris.common.metrics;

import com.tailoris.common.api.ApiResponse;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 业务指标调试接口（方便端到端测试脚本验证）
 *
 *   GET /api/metrics/snapshot     - 返回当前业务指标的 JSON 快照（不替代 Prometheus，
 *                                    仅用于脚本测试验证）
 *   POST /api/metrics/simulate    - 模拟 N 次登录失败（方便触发告警阈值测试）
 *
 * 注意：生产环境 Prometheus 采集应始终走 /actuator/prometheus，
 *      不要依赖本接口。
 */
@RestController
@RequestMapping("/api/metrics")
public class MetricsController {

    private final BusinessMetrics metrics;
    private final MetricsScheduler scheduler;

    public MetricsController(BusinessMetrics metrics, MetricsScheduler scheduler) {
        this.metrics = metrics;
        this.scheduler = scheduler;
    }

    @GetMapping("/snapshot")
    public ApiResponse<Map<String, Object>> snapshot() {
        // 调度一次 abnormal_rate 的即时刷新，便于测试脚本立刻看到变化
        metrics.refreshAbnormalRateFromWindow(5 * 60 * 1000L);
        scheduler.refreshActiveUsers();

        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("admin_login_count", (long) metrics.getAdminLoginCount());
        snapshot.put("failed_login_attempts", (long) metrics.getFailedLoginAttempts());
        snapshot.put("total_login_attempts", (long) metrics.getTotalLoginAttempts());
        snapshot.put("password_reset_requests", (long) metrics.getPasswordResetRequests());
        snapshot.put("business_active_users", metrics.getActiveUsers());
        snapshot.put("abnormal_login_rate_percent", metrics.getAbnormalLoginRate());
        snapshot.put("active_users_1h_ago", scheduler.getActiveUsersOneHourAgo());

        Map<String, Object> wrap = new LinkedHashMap<>();
        wrap.put("metrics", snapshot);
        return ApiResponse.ok(wrap);
    }

    /**
     * 模拟指定次数的失败登录，用于触发 "FailedLoginSpike / HighFailedLoginRate" 等告警规则
     * 例：curl -X POST http://localhost:8080/api/metrics/simulate?count=50
     */
    @PostMapping("/simulate")
    public ApiResponse<Map<String, Object>> simulate(@RequestParam(defaultValue = "20") int count,
                                                     @RequestParam(defaultValue = "user") String role) {
        String safeRole = "admin".equalsIgnoreCase(role) ? "admin" : "user";
        for (int i = 0; i < count; i++) {
            metrics.recordFailedLogin(safeRole, "sim-attacker-" + i, "wrong_password", "192.168.1.1");
        }
        // 立即刷新 abnormal_login_rate，使效果可见
        metrics.refreshAbnormalRateFromWindow(5 * 60 * 1000L);

        Map<String, Object> m = new HashMap<>();
        m.put("simulated_failed_logins", count);
        m.put("simulated_role", safeRole);
        m.put("failed_login_attempts_total", (long) metrics.getFailedLoginAttempts());
        m.put("abnormal_login_rate_now", metrics.getAbnormalLoginRate());
        return ApiResponse.ok(m);
    }
}
