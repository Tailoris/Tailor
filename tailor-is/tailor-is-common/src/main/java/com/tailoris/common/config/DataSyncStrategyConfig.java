package com.tailoris.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 数据同步策略配置 —— 按数据类型分类定义同步级别。
 *
 * <p>同步级别：</p>
 * <ul>
 *   <li>{@link SyncLevel#REAL_TIME} — 实时同步（秒级），使用 RabbitMQ + Seata</li>
 *   <li>{@link SyncLevel#NEAR_REAL_TIME} — 近实时同步（5 分钟级），定时轮询 + 最终一致性</li>
 *   <li>{@link SyncLevel#BATCH} — 批量同步（小时级），离线批处理</li>
 * </ul>
 *
 * <p>数据分类：</p>
 * <ul>
 *   <li>核心数据：订单、商品、资金、用户 → REAL_TIME</li>
 *   <li>非核心数据：社区帖子、学院课程、供应链数据、分析数据 → NEAR_REAL_TIME</li>
 *   <li>历史归档数据 → BATCH</li>
 * </ul>
 *
 * @author Tailor IS Team
 * @since 3.0.0
 */
@Data
@Component
@ConfigurationProperties(prefix = "tailoris.data-sync")
public class DataSyncStrategyConfig {

    /**
     * 同步级别枚举。
     */
    public enum SyncLevel {
        /** 实时同步：秒级，RabbitMQ + Seata 分布式事务 */
        REAL_TIME("实时同步", 0),

        /** 近实时同步：5 分钟级，定时任务 + 最终一致性 */
        NEAR_REAL_TIME("近实时同步", 300),

        /** 批量同步：小时级，离线批处理 */
        BATCH("批量同步", 3600);

        private final String label;
        private final int defaultIntervalSeconds;

        SyncLevel(String label, int defaultIntervalSeconds) {
            this.label = label;
            this.defaultIntervalSeconds = defaultIntervalSeconds;
        }

        public String getLabel() {
            return label;
        }

        public int getDefaultIntervalSeconds() {
            return defaultIntervalSeconds;
        }
    }

    /**
     * 核心数据类型集合（实时同步）。
     */
    private static final Set<String> REAL_TIME_DATA_TYPES = new HashSet<>(Arrays.asList(
            "order",          // 订单
            "product",        // 商品
            "fund",           // 资金
            "payment",        // 支付
            "user",           // 用户
            "merchant",       // 商户
            "settlement",     // 结算
            "inventory"       // 库存
    ));

    /**
     * 非核心数据类型集合（近实时同步）。
     */
    private static final Set<String> NEAR_REAL_TIME_DATA_TYPES = new HashSet<>(Arrays.asList(
            "community_post",     // 社区帖子
            "community_comment",  // 社区评论
            "community_like",     // 社区点赞
            "academy_course",     // 学院课程
            "supply",             // 供应链数据
            "analytics",          // 分析数据
            "message",            // 消息
            "notification"        // 通知
    ));

    /**
     * 批量同步数据类型集合。
     */
    private static final Set<String> BATCH_DATA_TYPES = new HashSet<>(Arrays.asList(
            "report",          // 报表
            "archive",         // 归档数据
            "log",             // 日志
            "audit_trail"      // 审计轨迹
    ));

    /**
     * 根据数据类型获取同步级别。
     *
     * @param dataType 数据类型标识（如 "order", "community_post"）
     * @return 对应的同步级别，未匹配时默认为 NEAR_REAL_TIME
     */
    public static SyncLevel getSyncLevel(String dataType) {
        if (dataType == null || dataType.isEmpty()) {
            return SyncLevel.NEAR_REAL_TIME;
        }
        String normalized = dataType.toLowerCase().trim();
        if (REAL_TIME_DATA_TYPES.contains(normalized)) {
            return SyncLevel.REAL_TIME;
        }
        if (NEAR_REAL_TIME_DATA_TYPES.contains(normalized)) {
            return SyncLevel.NEAR_REAL_TIME;
        }
        if (BATCH_DATA_TYPES.contains(normalized)) {
            return SyncLevel.BATCH;
        }
        // 未匹配的数据类型默认使用近实时同步（保守策略）
        return SyncLevel.NEAR_REAL_TIME;
    }

    /**
     * 判断数据类型是否为核心数据（需要实时同步）。
     *
     * @param dataType 数据类型
     * @return true 如果为核心数据
     */
    public static boolean isCoreData(String dataType) {
        return getSyncLevel(dataType) == SyncLevel.REAL_TIME;
    }

    /**
     * 获取所有核心数据类型。
     *
     * @return 核心数据类型不可变集合
     */
    public static Set<String> getCoreDataTypes() {
        return Set.copyOf(REAL_TIME_DATA_TYPES);
    }

    /**
     * 获取所有非核心数据类型。
     *
     * @return 非核心数据类型不可变集合
     */
    public static Set<String> getNonCoreDataTypes() {
        return Set.copyOf(NEAR_REAL_TIME_DATA_TYPES);
    }

    // ---- 可覆盖配置（通过 application.yml 扩展） ----

    /**
     * 额外实时同步数据类型（YAML 中追加）。
     */
    private Set<String> extraRealTimeTypes = Set.of();

    /**
     * 额外近实时同步数据类型。
     */
    private Set<String> extraNearRealTimeTypes = Set.of();

    /**
     * 额外批量同步数据类型。
     */
    private Set<String> extraBatchTypes = Set.of();

    /**
     * 注册后合并额外类型。
     */
    public void setExtraRealTimeTypes(Set<String> extraRealTimeTypes) {
        this.extraRealTimeTypes = extraRealTimeTypes;
        REAL_TIME_DATA_TYPES.addAll(extraRealTimeTypes);
    }

    public void setExtraNearRealTimeTypes(Set<String> extraNearRealTimeTypes) {
        this.extraNearRealTimeTypes = extraNearRealTimeTypes;
        NEAR_REAL_TIME_DATA_TYPES.addAll(extraNearRealTimeTypes);
    }

    public void setExtraBatchTypes(Set<String> extraBatchTypes) {
        this.extraBatchTypes = extraBatchTypes;
        BATCH_DATA_TYPES.addAll(extraBatchTypes);
    }
}
