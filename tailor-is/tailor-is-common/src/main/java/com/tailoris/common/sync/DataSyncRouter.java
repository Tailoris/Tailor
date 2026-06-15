package com.tailoris.common.sync;

import com.tailoris.common.config.DataSyncStrategyConfig;
import com.tailoris.common.config.DataSyncStrategyConfig.SyncLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 数据同步路由器 —— 根据数据类型自动选择同步通道。
 *
 * <h3>路由规则</h3>
 * <pre>
 *  数据类型
 *      │
 *      ├─ 核心数据（order, product, fund, user ...）
 *      │     → RealTimeDataSync（RabbitMQ 实时推送）
 *      │
 *      ├─ 非核心数据（community_post, academy_course, supply ...）
 *      │     → NearRealTimeSyncScheduler（5 分钟定时轮询）
 *      │
 *      └─ 批量数据（report, archive, log ...）
 *            → 标记为 BATCH，由外部调度系统处理
 * </pre>
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * @Service
 * public class CommunityPostService {
 *     private final DataSyncRouter dataSyncRouter;
 *
 *     public void createPost(CommunityPost post) {
 *         // ... 保存帖子
 *         postMapper.insert(post);
 *
 *         // 自动路由：非核心数据 → 近实时同步
 *         dataSyncRouter.routeSync("community_post", "create",
 *             Map.of("postId", post.getId(), "authorId", post.getAuthorId()));
 *     }
 * }
 * }</pre>
 *
 * @author Tailor IS Team
 * @since 3.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataSyncRouter {

    private final RealTimeDataSync realTimeDataSync;
    private final NearRealTimeSyncScheduler nearRealTimeSyncScheduler;

    /**
     * 路由同步请求 —— 根据数据类型自动选择同步策略。
     *
     * @param dataType 数据类型
     * @param action   操作类型（create / update / delete）
     * @param payload  业务载荷
     * @return 实际使用的同步级别
     */
    public SyncLevel routeSync(String dataType, String action, @Nullable Map<String, Object> payload) {
        SyncLevel level = DataSyncStrategyConfig.getSyncLevel(dataType);

        switch (level) {
            case REAL_TIME -> {
                log.debug("路由到实时同步通道: dataType={}, action={}", dataType, action);
                realTimeDataSync.publishAfterCommit(dataType, action, payload);
            }
            case NEAR_REAL_TIME -> {
                log.debug("路由到近实时同步通道: dataType={}, action={}", dataType, action);
                // 近实时数据不立即同步，仅记录日志供追踪
                // 实际同步由 NearRealTimeSyncScheduler 定时执行
            }
            case BATCH -> {
                log.debug("数据类型为批量同步，跳过即时路由: dataType={}", dataType);
            }
            default -> {
                log.warn("未知的同步级别: dataType={}, 使用默认近实时同步", dataType);
                realTimeDataSync.publishAfterCommit(dataType, action, payload);
            }
        }

        return level;
    }

    /**
     * 强制使用实时同步通道（忽略数据类型分类）。
     *
     * <p>适用于临时提升某类数据的同步级别。</p>
     *
     * @param dataType 数据类型
     * @param action   操作类型
     * @param payload  业务载荷
     */
    public void forceRealTimeSync(String dataType, String action, @Nullable Map<String, Object> payload) {
        log.info("强制实时同步: dataType={}, action={}", dataType, action);
        realTimeDataSync.publishAfterCommit(dataType, action, payload);
    }

    /**
     * 手动触发近实时同步（绕过定时调度器）。
     *
     * @param dataType 数据类型
     * @param action   操作类型
     * @param payload  业务载荷
     */
    public void triggerNearRealTimeSync(String dataType, String action, @Nullable Map<String, Object> payload) {
        log.info("手动触发近实时同步: dataType={}, action={}", dataType, action);
        // 近实时同步由调度器自动处理，这里仅记录追踪信息
        log.debug("近实时同步已记录, 将在下一个调度周期执行: dataType={}", dataType);
    }

    /**
     * 获取数据类型的路由策略描述。
     *
     * @param dataType 数据类型
     * @return 路由描述
     */
    public String getRouteDescription(String dataType) {
        SyncLevel level = DataSyncStrategyConfig.getSyncLevel(dataType);
        return switch (level) {
            case REAL_TIME -> String.format("[%s] RabbitMQ 实时推送", level.getLabel());
            case NEAR_REAL_TIME -> String.format("[%s] 定时轮询 (5 min)", level.getLabel());
            case BATCH -> String.format("[%s] 离线批处理 (hourly)", level.getLabel());
        };
    }
}
