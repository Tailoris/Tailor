package com.tailoris.ai.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tailoris.ai.config.CloudModelConfig;
import com.tailoris.ai.dto.PatternGenerateRequest;
import com.tailoris.ai.dto.PatternGenerateResponse;
import com.tailoris.ai.entity.PatternRecord;
import com.tailoris.ai.mapper.PatternRecordMapper;
import com.tailoris.ai.service.PatternGenerationStrategy;
import com.tailoris.common.config.CacheRouter;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 非高峰时段批量纸样生成器。
 *
 * <p>在凌晨非高峰时段（默认 2:00-6:00 AM）自动预生成热门纸样，
 * 将结果缓存到 Redis，供高峰时段快速检索。</p>
 *
 * <h3>工作原理：</h3>
 * <ol>
 *   <li>凌晨 2:00 触发批量生成任务</li>
 *   <li>查询热门款式类型和最常用尺寸组合</li>
 *   <li>使用云端模型批量生成纸样</li>
 *   <li>将结果缓存到 Redis（按款式+尺寸索引）</li>
 *   <li>高峰时段用户请求可直接从缓存获取</li>
 * </ol>
 *
 * <h3>配置项：</h3>
 * <ul>
 *   <li>enabled: 是否启用非高峰批量生成</li>
 *   <li>cron: Cron 表达式（默认每天 2:00 AM）</li>
 *   <li>batch-size: 每批次生成的纸样数量</li>
 *   <li>cache-ttl-hours: 缓存过期时间（小时）</li>
 *   <li>target-pattern-types: 目标纸样类型列表</li>
 * </ul>
 *
 * @author Tailor IS Team
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "tailoris.ai.off-peak-batch", name = "enabled", havingValue = "true", matchIfMissing = false)
public class OffPeakBatchGenerator {

    private final PatternGenerationStrategy patternGenerationStrategy;
    private final PatternRecordMapper patternRecordMapper;
    private final CloudModelConfig cloudModelConfig;
    private final CacheRouter cacheRouter;

    /** 缓存 key 前缀 */
    private static final String CACHE_KEY_PREFIX = "ai:pattern:prewarm:";

    /** 热门款式列表 */
    private static final String[] DEFAULT_POPULAR_TYPES = {"DRESS", "JACKET", "SHIRT", "PANTS"};

    /** 热门尺寸列表（标准尺码） */
    private static final String[] DEFAULT_POPULAR_SIZES = {"S", "M", "L", "XL"};

    /** 默认每批次数量 */
    private int batchSize = 50;

    /** 缓存过期时间（小时） */
    private int cacheTtlHours = 24;

    /** 目标纸样类型（可配置覆盖默认值） */
    private List<String> targetPatternTypes = new ArrayList<>();

    /**
     * 每天凌晨 2:00 执行批量预生成。
     *
     * <p>可通过配置文件修改 cron 表达式：</p>
     * <pre>
     * tailor-is:
     *   ai:
     *     off-peak-batch:
     *       cron: "0 0 3 * * ?"  # 改为凌晨 3:00
     * </pre>
     */
    @Scheduled(cron = "${tailoris.ai.off-peak-batch.cron:0 0 2 * * ?}")
    public void executeBatchGeneration() {
        log.info("[OffPeakBatchGenerator] ===== 非高峰批量生成任务开始 =====");
        LocalDateTime startTime = LocalDateTime.now();

        try {
            // 1. 确定需要预生成的款式类型
            List<String> types = getTargetTypes();
            log.info("[OffPeakBatchGenerator] 目标款式类型: {}", types);

            // 2. 逐个类型批量生成
            int totalGenerated = 0;
            int totalCached = 0;

            for (String type : types) {
                BatchResult result = generateForType(type);
                totalGenerated += result.getGenerated();
                totalCached += result.getCached();

                log.info("[OffPeakBatchGenerator] 款式 {} 完成: 生成={}, 缓存={}",
                        type, result.getGenerated(), result.getCached());
            }

            LocalDateTime endTime = LocalDateTime.now();
            Duration duration = Duration.between(startTime, endTime);

            log.info("[OffPeakBatchGenerator] ===== 批量生成任务完成 =====");
            log.info("[OffPeakBatchGenerator] 总生成: {}, 总缓存: {}, 耗时: {}秒",
                    totalGenerated, totalCached, duration.getSeconds());

            // 记录统计到 Redis
            recordBatchStats(totalGenerated, totalCached, duration.getSeconds());

        } catch (Exception e) {
            log.error("[OffPeakBatchGenerator] 批量生成任务异常: {}", e.getMessage(), e);
        }
    }

    /**
     * 为指定款式类型批量生成纸样。
     *
     * @param garmentType 款式类型
     * @return 批量生成结果
     */
    private BatchResult generateForType(String garmentType) {
        BatchResult result = new BatchResult();
        RedisTemplate<String, Object> template = cacheRouter.getCoreTemplate();

        // 查询该类型的已有纸样（用于确定生成参数范围）
        LambdaQueryWrapper<PatternRecord> query = new LambdaQueryWrapper<>();
        query.eq(PatternRecord::getStatus, 1)
                .eq(PatternRecord::getPatternType, getPatternTypeCode(garmentType))
                .orderByDesc(PatternRecord::getCreateTime)
                .last("LIMIT " + batchSize);

        List<PatternRecord> existingPatterns = patternRecordMapper.selectList(query);

        // 为每个已有模式生成变体
        for (PatternRecord basePattern : existingPatterns) {
            try {
                PatternGenerateRequest request = buildVariantRequest(basePattern, garmentType);
                PatternGenerateResponse response = patternGenerationStrategy.generatePattern(request);

                if (response != null && response.getPatternId() != null) {
                    result.incrementGenerated();

                    // 缓存到 Redis
                    String cacheKey = buildCacheKey(garmentType, basePattern.getId());
                    template.opsForValue().set(cacheKey, response, Duration.ofHours(cacheTtlHours));
                    result.incrementCached();

                    log.debug("[OffPeakBatchGenerator] 已缓存: key={}", cacheKey);
                }
            } catch (Exception e) {
                log.warn("[OffPeakBatchGenerator] 单个纸样生成失败: baseId={}, error={}",
                        basePattern.getId(), e.getMessage());
            }

            // 如果已达到批次上限，提前结束
            if (result.getGenerated() >= batchSize) {
                break;
            }
        }

        return result;
    }

    /**
     * 构建变体生成请求。
     */
    private PatternGenerateRequest buildVariantRequest(PatternRecord basePattern, String garmentType) {
        return PatternGenerateRequest.builder()
                .patternName(basePattern.getPatternName() + "-prewarm")
                .garmentType(garmentType)
                .bodySizeId(basePattern.getBodySizeId())
                .patternType(basePattern.getPatternType())
                .parameters(basePattern.getParameters())
                .exportFormat("SVG")
                .build();
    }

    /**
     * 获取目标款式类型列表。
     */
    private List<String> getTargetTypes() {
        if (targetPatternTypes != null && !targetPatternTypes.isEmpty()) {
            return targetPatternTypes;
        }
        return List.of(DEFAULT_POPULAR_TYPES);
    }

    /**
     * 构建缓存 key。
     */
    private String buildCacheKey(String garmentType, Long basePatternId) {
        return CACHE_KEY_PREFIX + garmentType.toLowerCase() + ":" + basePatternId;
    }

    /**
     * 获取纸样类型代码。
     */
    private Integer getPatternTypeCode(String garmentType) {
        return switch (garmentType.toUpperCase()) {
            case "SHIRT" -> 5;
            case "PANTS" -> 2;
            case "DRESS" -> 3;
            case "JACKET" -> 4;
            default -> 1;
        };
    }

    /**
     * 记录批量生成统计到 Redis。
     */
    private void recordBatchStats(int totalGenerated, int totalCached, long durationSeconds) {
        try {
            RedisTemplate<String, Object> template = cacheRouter.getCoreTemplate();
            String dateKey = LocalDateTime.now().toLocalDate().toString();
            String statsKey = "ai:offpeak:stats:" + dateKey;

            template.opsForHash().put(statsKey, "generated", totalGenerated);
            template.opsForHash().put(statsKey, "cached", totalCached);
            template.opsForHash().put(statsKey, "duration_seconds", durationSeconds);
            template.opsForHash().put(statsKey, "timestamp", LocalDateTime.now().toString());
            template.expire(statsKey, Duration.ofDays(30));
        } catch (Exception e) {
            log.warn("[OffPeakBatchGenerator] 统计记录失败: {}", e.getMessage());
        }
    }

    /**
     * 从缓存获取预生成的纸样。
     *
     * @param garmentType 款式类型
     * @param basePatternId 基础纸样ID
     * @return 预生成的纸样，如果不存在则返回 null
     */
    public PatternGenerateResponse getCachedPattern(String garmentType, Long basePatternId) {
        try {
            RedisTemplate<String, Object> template = cacheRouter.getCoreTemplate();
            String cacheKey = buildCacheKey(garmentType, basePatternId);
            Object cached = template.opsForValue().get(cacheKey);

            if (cached instanceof PatternGenerateResponse response) {
                log.debug("[OffPeakBatchGenerator] 缓存命中: key={}", cacheKey);
                return response;
            }
            log.debug("[OffPeakBatchGenerator] 缓存未命中: key={}", cacheKey);
        } catch (Exception e) {
            log.warn("[OffPeakBatchGenerator] 缓存读取失败: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 批量生成结果统计。
     */
    @Data
    public static class BatchResult {
        private int generated = 0;
        private int cached = 0;

        public void incrementGenerated() {
            this.generated++;
        }

        public void incrementCached() {
            this.cached++;
        }
    }
}
