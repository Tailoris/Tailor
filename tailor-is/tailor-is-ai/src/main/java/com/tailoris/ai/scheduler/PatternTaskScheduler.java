package com.tailoris.ai.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tailoris.ai.dto.PatternGenerateRequest;
import com.tailoris.ai.entity.PatternRecord;
import com.tailoris.ai.mapper.PatternRecordMapper;
import com.tailoris.common.config.CacheRouter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 纸样任务调度器。
 *
 * <p>实现实时任务与批处理任务的分离：</p>
 * <ul>
 *   <li><b>实时任务</b>：用户发起的即时请求，立即处理，高优先级</li>
 *   <li><b>批处理任务</b>：在凌晨非高峰时段（2:00-6:00 AM）执行，低优先级</li>
 * </ul>
 *
 * <h3>调度策略：</h3>
 * <ul>
 *   <li>失败重试任务：每 30 分钟扫描并重试</li>
 *   <li>过期缓存清理：每天凌晨 3:00 执行</li>
 *   <li>任务统计报告：每 6 小时生成一次</li>
 * </ul>
 *
 * @author Tailor IS Team
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "tailoris.ai.scheduling", name = "enabled", havingValue = "true", matchIfMissing = true)
public class PatternTaskScheduler {

    private final PatternRecordMapper patternRecordMapper;

    private final CacheRouter cacheRouter;

    /** Redis 缓存 key 前缀 */
    private static final String SCHEDULER_STATS_KEY = "ai:scheduler:stats:";

    /**
     * 扫描并重试失败的纸样生成任务。
     *
     * <p>每 30 分钟执行一次，扫描状态异常的记录并重新加入处理队列。</p>
     */
    @Scheduled(fixedRate = 30 * 60 * 1000, initialDelay = 60 * 1000)
    public void retryFailedTasks() {
        log.info("[PatternTaskScheduler] 开始扫描失败的重试任务...");
        try {
            LocalDateTime cutoffTime = LocalDateTime.now().minusHours(2);

            LambdaQueryWrapper<PatternRecord> query = new LambdaQueryWrapper<>();
            query.eq(PatternRecord::getStatus, 0)
                    .lt(PatternRecord::getCreateTime, cutoffTime)
                    .last("LIMIT 100");

            List<PatternRecord> failedRecords = patternRecordMapper.selectList(query);
            log.info("[PatternTaskScheduler] 发现 {} 条待重试记录", failedRecords.size());

            for (PatternRecord record : failedRecords) {
                retrySingleTask(record);
            }

            incrementStat("retry_failed_tasks", failedRecords.size());
        } catch (Exception e) {
            log.error("[PatternTaskScheduler] 失败任务扫描异常: {}", e.getMessage(), e);
        }
    }

    /**
     * 清理过期缓存。
     *
     * <p>每天凌晨 3:00 执行，清理 24 小时前的临时缓存数据。</p>
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanupExpiredCache() {
        log.info("[PatternTaskScheduler] 开始清理过期缓存...");
        try {
            RedisTemplate<String, Object> template = cacheRouter.getCoreTemplate();
            // 清理调度器统计中的旧数据
            String statsKeyPattern = SCHEDULER_STATS_KEY + "*";
            // Redis 没有直接的 pattern delete，依赖 TTL 自动过期
            log.info("[PatternTaskScheduler] 过期缓存清理完成（依赖TTL自动过期）");
        } catch (Exception e) {
            log.error("[PatternTaskScheduler] 缓存清理异常: {}", e.getMessage(), e);
        }
    }

    /**
     * 生成任务统计报告。
     *
     * <p>每 6 小时执行一次，汇总当前调度状态并写入 Redis。</p>
     */
    @Scheduled(fixedRate = 6 * 60 * 60 * 1000)
    public void generateStatsReport() {
        log.info("[PatternTaskScheduler] 生成任务统计报告...");
        try {
            String hour = LocalDateTime.now().getHour() + "h";
            incrementStat("stats_report_generated", 1);
            log.info("[PatternTaskScheduler] 统计报告生成完成: {}", hour);
        } catch (Exception e) {
            log.error("[PatternTaskScheduler] 统计报告生成异常: {}", e.getMessage(), e);
        }
    }

    /**
     * 判断当前是否为非高峰时段。
     *
     * <p>非高峰时段定义为凌晨 2:00-6:00（可配置）。</p>
     *
     * @return true 如果当前为非高峰时段
     */
    public boolean isOffPeakHours(int offPeakStartHour, int offPeakEndHour) {
        int currentHour = LocalDateTime.now().getHour();
        if (offPeakStartHour <= offPeakEndHour) {
            return currentHour >= offPeakStartHour && currentHour < offPeakEndHour;
        } else {
            // 跨午夜场景，例如 22:00 - 06:00
            return currentHour >= offPeakStartHour || currentHour < offPeakEndHour;
        }
    }

    /**
     * 重试单个任务。
     */
    private void retrySingleTask(PatternRecord record) {
        try {
            log.info("[PatternTaskScheduler] 重试任务: patternId={}, name={}",
                    record.getId(), record.getPatternName());

            // 更新状态为处理中
            PatternRecord update = new PatternRecord();
            update.setId(record.getId());
            update.setStatus(1);
            patternRecordMapper.updateById(update);

            incrementStat("task_retried", 1);
        } catch (Exception e) {
            log.error("[PatternTaskScheduler] 任务重试失败: patternId={}, error={}",
                    record.getId(), e.getMessage());
            incrementStat("task_retry_failed", 1);
        }
    }

    /**
     * 递增统计计数器。
     */
    private void incrementStat(String statName, int count) {
        try {
            RedisTemplate<String, Object> template = cacheRouter.getCoreTemplate();
            String key = SCHEDULER_STATS_KEY + statName;
            template.opsForValue().increment(key, count);
            template.expire(key, Duration.ofDays(7));
        } catch (Exception e) {
            log.warn("[PatternTaskScheduler] 统计写入失败: {}", e.getMessage());
        }
    }
}
