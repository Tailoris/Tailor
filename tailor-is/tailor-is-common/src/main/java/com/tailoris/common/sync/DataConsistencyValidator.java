package com.tailoris.common.sync;

import com.tailoris.common.config.DataSyncStrategyConfig;
import com.tailoris.common.config.DataSyncStrategyConfig.SyncLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.lang.Nullable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 数据一致性校验工具 —— 周期性验证各服务间数据一致性并报告差异。
 *
 * <h3>校验机制</h3>
 * <ol>
 *   <li><b>采样比对</b>：从源服务和目标服务各抽取样本记录进行比对</li>
 *   <li><b>校验和对比</b>：计算关键字段校验和，快速发现不一致</li>
 *   <li><b>自动修复</b>：非核心数据差异自动触发重新同步</li>
 *   <li><b>告警上报</b>：核心数据差异记录并告警，需人工介入</li>
 * </ol>
 *
 * <h3>扩展方式</h3>
 * 实现 {@link ConsistencyChecker} 接口并注册为 Spring Bean。
 *
 * @author Tailor IS Team
 * @since 3.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "tailoris.data-sync.validation.enabled", matchIfMissing = true)
public class DataConsistencyValidator {

    private final List<ConsistencyChecker> checkers;

    /** 校验历史记录 */
    private final List<ValidationReport> recentReports = Collections.synchronizedList(new ArrayList<>());

    /** 统计 */
    private final AtomicInteger totalValidations = new AtomicInteger(0);
    private final AtomicInteger totalDiscrepancies = new AtomicInteger(0);
    private final AtomicInteger totalAutoRepairs = new AtomicInteger(0);

    /** 最大保留报告数 */
    private static final int MAX_REPORTS = 100;

    /**
     * 一致性校验器接口。
     */
    public interface ConsistencyChecker {

        /**
         * 数据类型标识。
         */
        String getDataType();

        /**
         * 执行一致性校验。
         *
         * @return 校验结果
         */
        CheckResult check();

        /**
         * 自动修复不一致（仅适用于非核心数据）。
         *
         * @param recordId 不一致的记录 ID
         * @return 是否修复成功
         */
        default boolean autoRepair(String recordId) {
            log.warn("数据类型 {} 未实现自动修复, recordId={}", getDataType(), recordId);
            return false;
        }
    }

    /**
     * 校验结果。
     */
    public record CheckResult(
            boolean consistent,
            int totalChecked,
            int discrepancies,
            List<String> discrepancyIds,
            String details
    ) {
        public static CheckResult ok(int checked) {
            return new CheckResult(true, checked, 0, List.of(), "一致");
        }

        public static CheckResult fail(int checked, List<String> ids, String details) {
            return new CheckResult(false, checked, ids.size(), ids, details);
        }
    }

    /**
     * 校验报告。
     */
    public record ValidationReport(
            String dataType,
            boolean consistent,
            int checked,
            int discrepancies,
            List<String> discrepancyIds,
            boolean autoRepairAttempted,
            int autoRepairSuccess,
            Duration duration,
            LocalDateTime timestamp
    ) {}

    /**
     * 定时执行一致性校验。
     *
     * <p>默认每小时执行一次，可通过 {@code tailoris.data-sync.validation.cron} 配置。</p>
     */
    @Scheduled(cron = "${tailoris.data-sync.validation.cron:0 0 * * * ?}")
    public void validateAll() {
        if (checkers == null || checkers.isEmpty()) {
            log.debug("无一致性校验器，跳过校验任务");
            return;
        }

        log.info("数据一致性校验任务开始, checker 数量={}", checkers.size());
        LocalDateTime start = LocalDateTime.now();

        for (ConsistencyChecker checker : checkers) {
            try {
                validateChecker(checker);
            } catch (Exception e) {
                log.error("一致性校验异常, dataType={}", checker.getDataType(), e);
            }
        }

        Duration elapsed = Duration.between(start, LocalDateTime.now());
        log.info("数据一致性校验任务完成, 耗时={}", elapsed);
    }

    private void validateChecker(ConsistencyChecker checker) {
        String dataType = checker.getDataType();
        log.info("开始校验数据类型: {}", dataType);
        LocalDateTime checkStart = LocalDateTime.now();

        CheckResult result;
        try {
            result = checker.check();
        } catch (Exception e) {
            log.error("校验执行失败, dataType={}", dataType, e);
            result = CheckResult.fail(0, List.of(), "校验异常: " + e.getMessage());
        }

        Duration duration = Duration.between(checkStart, LocalDateTime.now());

        totalValidations.incrementAndGet();
        totalDiscrepancies.addAndGet(result.discrepancies());

        boolean autoRepairAttempted = false;
        int autoRepairSuccess = 0;

        // 非核心数据自动修复
        if (!result.consistent() && !DataSyncStrategyConfig.isCoreData(dataType)) {
            autoRepairAttempted = true;
            for (String id : result.discrepancyIds()) {
                try {
                    if (checker.autoRepair(id)) {
                        autoRepairSuccess++;
                        totalAutoRepairs.incrementAndGet();
                        log.info("自动修复成功, dataType={}, recordId={}", dataType, id);
                    } else {
                        log.warn("自动修复失败, dataType={}, recordId={}", dataType, id);
                    }
                } catch (Exception e) {
                    log.error("自动修复异常, dataType={}, recordId={}", dataType, id, e);
                }
            }
        }

        // 核心数据不一致记录告警
        if (!result.consistent() && DataSyncStrategyConfig.isCoreData(dataType)) {
            log.error("核心数据不一致告警: dataType={}, discrepancies={}, ids={}",
                    dataType, result.discrepancies(), result.discrepancyIds());
        }

        // 保存报告
        ValidationReport report = new ValidationReport(
                dataType,
                result.consistent(),
                result.totalChecked(),
                result.discrepancies(),
                result.discrepancyIds(),
                autoRepairAttempted,
                autoRepairSuccess,
                duration,
                LocalDateTime.now()
        );

        recentReports.add(report);
        // 只保留最近 N 条报告
        while (recentReports.size() > MAX_REPORTS) {
            recentReports.remove(0);
        }

        log.info("校验完成: dataType={}, consistent={}, checked={}, discrepancies={}, duration={}",
                dataType, result.consistent(), result.totalChecked(),
                result.discrepancies(), duration);
    }

    /**
     * 获取最近的校验报告。
     *
     * @param limit 最大返回数量
     * @return 校验报告列表（按时间倒序）
     */
    public List<ValidationReport> getRecentReports(int limit) {
        List<ValidationReport> copy = new ArrayList<>(recentReports);
        copy.sort(Comparator.comparing(ValidationReport::timestamp).reversed());
        return copy.subList(0, Math.min(limit, copy.size()));
    }

    /**
     * 获取校验统计摘要。
     *
     * @return 摘要 Map
     */
    public Map<String, Object> getStats() {
        return Map.of(
                "totalValidations", totalValidations.get(),
                "totalDiscrepancies", totalDiscrepancies.get(),
                "totalAutoRepairs", totalAutoRepairs.get(),
                "recentReportCount", recentReports.size()
        );
    }

    /**
     * 手动触发指定数据类型的校验。
     *
     * @param dataType 数据类型
     * @return 校验报告，未找到对应 checker 时返回 null
     */
    @Nullable
    public ValidationReport validateNow(String dataType) {
        Optional<ConsistencyChecker> optional = checkers.stream()
                .filter(c -> c.getDataType().equals(dataType))
                .findFirst();

        if (optional.isEmpty()) {
            log.warn("未找到数据类型 {} 的校验器", dataType);
            return null;
        }

        validateChecker(optional.get());
        return recentReports.isEmpty() ? null : recentReports.get(recentReports.size() - 1);
    }
}
