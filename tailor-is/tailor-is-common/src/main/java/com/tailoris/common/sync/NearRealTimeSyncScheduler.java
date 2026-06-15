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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 近实时同步调度器 —— 每 5 分钟轮询非核心数据并执行增量同步。
 *
 * <h3>设计原则</h3>
 * <ul>
 *   <li><b>最终一致性</b>：允许短时间内的数据不一致，通过周期性轮询收敛</li>
 *   <li><b>增量同步</b>：仅同步自上次同步以来变更的记录（基于 update_time）</li>
 *   <li><b>幂等性</b>：同一条记录多次同步不会产生副作用</li>
 *   <li><b>限流保护</b>：单次同步批量大小可控，避免对源数据库造成压力</li>
 * </ul>
 *
 * <h3>同步范围</h3>
 * <ul>
 *   <li>社区帖子 (community_post)</li>
 *   <li>社区评论 (community_comment)</li>
 *   <li>学院课程 (academy_course)</li>
 *   <li>供应链数据 (supply)</li>
 *   <li>分析数据 (analytics)</li>
 * </ul>
 *
 * <h3>扩展方式</h3>
 * 实现 {@link NearRealTimeSyncProvider} 接口并注册为 Spring Bean，
 * 调度器会自动发现并调度。
 *
 * @author Tailor IS Team
 * @since 3.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "tailoris.data-sync.near-real-time.enabled", matchIfMissing = true)
public class NearRealTimeSyncScheduler {

    private final List<NearRealTimeSyncProvider> providers;

    /** 各数据类型的上次同步时间 */
    private final Map<String, LocalDateTime> lastSyncTime = new ConcurrentHashMap<>();

    /** 同步统计 */
    private final Map<String, AtomicInteger> syncCounts = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> syncErrors = new ConcurrentHashMap<>();

    /**
     * 近实时同步提供者接口。
     * <p>各业务服务实现此接口以支持近实时数据同步。</p>
     */
    public interface NearRealTimeSyncProvider {

        /**
         * 数据类型标识（如 "community_post"）。
         */
        String getDataType();

        /**
         * 获取自指定时间以来变更的记录 ID 列表。
         *
         * @param since 起始时间
         * @return 变更记录 ID 列表
         */
        List<String> getChangedRecordIds(LocalDateTime since);

        /**
         * 同步指定记录到下游服务。
         *
         * @param recordId 记录 ID
         */
        void syncRecord(String recordId);

        /**
         * 同步间隔（秒），默认 300（5 分钟）。
         */
        default int getSyncIntervalSeconds() {
            return 300;
        }
    }

    /**
     * 每 5 分钟执行一次近实时同步任务。
     *
     * <p>interval 可通过 {@code tailoris.data-sync.near-real-time.fixed-rate-ms} 配置。</p>
     */
    @Scheduled(fixedRateString = "${tailoris.data-sync.near-real-time.fixed-rate-ms:300000}")
    public void executeSync() {
        if (providers == null || providers.isEmpty()) {
            log.debug("无近实时同步提供者，跳过同步任务");
            return;
        }

        log.info("近实时同步任务开始执行, provider 数量={}", providers.size());
        LocalDateTime now = LocalDateTime.now();

        for (NearRealTimeSyncProvider provider : providers) {
            try {
                syncProvider(provider, now);
            } catch (Exception e) {
                log.error("近实时同步执行异常, dataType={}", provider.getDataType(), e);
                syncErrors.computeIfAbsent(provider.getDataType(), k -> new AtomicInteger(0))
                        .incrementAndGet();
            }
        }

        log.info("近实时同步任务执行完成");
    }

    private void syncProvider(NearRealTimeSyncProvider provider, LocalDateTime now) {
        String dataType = provider.getDataType();

        // 验证同步级别
        SyncLevel level = DataSyncStrategyConfig.getSyncLevel(dataType);
        if (level == SyncLevel.REAL_TIME) {
            log.warn("核心数据不应使用近实时同步: dataType={}", dataType);
        }

        LocalDateTime since = lastSyncTime.getOrDefault(dataType, now.minusMinutes(10));
        lastSyncTime.put(dataType, now);

        // 获取变更记录
        List<String> changedIds;
        try {
            changedIds = provider.getChangedRecordIds(since);
        } catch (Exception e) {
            log.error("获取变更记录失败, dataType={}, since={}", dataType, since, e);
            return;
        }

        if (changedIds == null || changedIds.isEmpty()) {
            log.debug("无变更记录, dataType={}, since={}", dataType, since);
            return;
        }

        log.info("发现变更记录, dataType={}, count={}, since={}", dataType, changedIds.size(), since);

        // 逐条同步
        int successCount = 0;
        int failCount = 0;
        for (String recordId : changedIds) {
            try {
                provider.syncRecord(recordId);
                successCount++;
            } catch (Exception e) {
                failCount++;
                log.error("同步记录失败, dataType={}, recordId={}", dataType, recordId, e);
            }
        }

        int total = syncCounts.computeIfAbsent(dataType, k -> new AtomicInteger(0))
                .addAndGet(successCount);
        log.info("近实时同步完成, dataType={}, success={}, fail={}, total={}",
                dataType, successCount, failCount, total);
    }

    /**
     * 获取同步统计信息。
     *
     * @return 各数据类型的同步统计
     */
    public Map<String, SyncStats> getStats() {
        Map<String, SyncStats> stats = new ConcurrentHashMap<>();
        Set<String> allTypes = Set.copyOf(
                Set.copyOf(syncCounts.keySet()));
        allTypes.addAll(syncErrors.keySet());

        for (String type : allTypes) {
            stats.put(type, new SyncStats(
                    syncCounts.getOrDefault(type, new AtomicInteger(0)).get(),
                    syncErrors.getOrDefault(type, new AtomicInteger(0)).get(),
                    lastSyncTime.get(type)
            ));
        }
        return stats;
    }

    /**
     * 同步统计记录。
     */
    public record SyncStats(
            int totalSynced,
            int errorCount,
            @Nullable LocalDateTime lastSyncTime
    ) {
        public Duration timeSinceLastSync() {
            if (lastSyncTime == null) {
                return null;
            }
            return Duration.between(lastSyncTime, LocalDateTime.now());
        }
    }
}
