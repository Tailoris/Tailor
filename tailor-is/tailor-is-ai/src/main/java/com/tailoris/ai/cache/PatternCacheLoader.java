package com.tailoris.ai.cache;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tailoris.ai.entity.PatternRecord;
import com.tailoris.ai.mapper.PatternRecordMapper;
import com.tailoris.common.config.CacheRouter;
import com.tailoris.common.util.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.util.List;

/**
 * AI 图案数据预加载到 Redis 缓存。
 *
 * <p>在应用启动时加载基础图案数据库到 Redis Cluster，
 * 每 30 分钟定时刷新缓存。
 *
 * <p>缓存 key 设计:
 * <ul>
 *   <li>{@code pattern:base:{type}:{size}} — 按类型和尺寸缓存图案列表</li>
 *   <li>{@code pattern:base:all} — 所有图案元数据</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnBean(CacheRouter.class)
public class PatternCacheLoader {

    private final CacheRouter cacheRouter;
    private final PatternRecordMapper patternRecordMapper;

    private static final String CACHE_KEY_PREFIX = "pattern:base:";
    private static final String CACHE_KEY_ALL = CACHE_KEY_PREFIX + "all";

    /**
     * 应用启动时预加载图案数据到 Redis
     */
    @PostConstruct
    public void loadPatternsOnStartup() {
        log.info("[PatternCacheLoader] 开始预加载图案数据到 Redis...");
        try {
            loadAllPatterns();
            loadPatternsByType();
            log.info("[PatternCacheLoader] 图案数据预加载完成");
        } catch (Exception e) {
            log.error("[PatternCacheLoader] 图案数据预加载失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 每 30 分钟刷新一次缓存
     */
    @Scheduled(fixedRate = 30 * 60 * 1000)
    public void refreshPatterns() {
        log.info("[PatternCacheLoader] 开始刷新图案缓存...");
        try {
            loadAllPatterns();
            loadPatternsByType();
            log.info("[PatternCacheLoader] 图案缓存刷新完成");
        } catch (Exception e) {
            log.error("[PatternCacheLoader] 图案缓存刷新失败: {}", e.getMessage(), e);
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    // Loaders
    // ──────────────────────────────────────────────────────────────────────

    /**
     * 加载所有图案元数据到 pattern:base:all
     */
    private void loadAllPatterns() {
        RedisTemplate<String, Object> template = cacheRouter.getCoreTemplate();

        LambdaQueryWrapper<PatternRecord> query = new LambdaQueryWrapper<>();
        query.eq(PatternRecord::getStatus, 1)
                .select(PatternRecord::getId, PatternRecord::getPatternName,
                        PatternRecord::getPatternType, PatternRecord::getThumbnailUrl,
                        PatternRecord::getPatternFileUrl);

        List<PatternRecord> patterns = patternRecordMapper.selectList(query);

        String key = CACHE_KEY_ALL;
        template.opsForValue().set(key, patterns, Duration.ofHours(1));
        log.info("[PatternCacheLoader] 已缓存 {} 个图案元数据到 {}", patterns.size(), key);
    }

    /**
     * 按类型加载图案到 pattern:base:{type}:{size}
     */
    private void loadPatternsByType() {
        RedisTemplate<String, Object> template = cacheRouter.getCoreTemplate();

        // 查询所有不重复的 patternType
        List<PatternRecord> allTypes = patternRecordMapper.selectList(
                new LambdaQueryWrapper<PatternRecord>()
                        .eq(PatternRecord::getStatus, 1)
                        .select(PatternRecord::getPatternType)
        );

        for (PatternRecord record : allTypes) {
            Integer type = record.getPatternType();
            if (type == null) continue;

            String typeKey = CACHE_KEY_PREFIX + "type:" + type;
            LambdaQueryWrapper<PatternRecord> query = new LambdaQueryWrapper<>();
            query.eq(PatternRecord::getPatternType, type)
                    .eq(PatternRecord::getStatus, 1)
                    .select(PatternRecord::getId, PatternRecord::getPatternName,
                            PatternRecord::getThumbnailUrl, PatternRecord::getPatternFileUrl);

            List<PatternRecord> typePatterns = patternRecordMapper.selectList(query);

            template.opsForValue().set(typeKey, typePatterns, Duration.ofHours(1));
            log.debug("[PatternCacheLoader] 已缓存 patternType={} 的 {} 个图案到 {}", type, typePatterns.size(), typeKey);
        }
    }
}
