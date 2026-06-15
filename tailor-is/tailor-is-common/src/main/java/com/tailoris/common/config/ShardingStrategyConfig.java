package com.tailoris.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 数据库分片策略配置 — 定义核心与非核心服务的数据源路由策略。
 *
 * <h3>设计目标</h3>
 * <ul>
 *   <li>核心服务（订单、支付、AI、版权）→ TiDB + ShardingSphere 分片</li>
 *   <li>非核心服务（社区、学院、供应链）→ MySQL 主从架构</li>
 * </ul>
 *
 * <h3>配置示例</h3>
 * <pre>
 * tailoris:
 *   sharding:
 *     enabled: true
 *     shard-count: 4
 *     sharding-column: merchant_id
 *     core-services:
 *       - order
 *       - payment
 *       - ai
 *       - copyright
 *     non-core-services:
 *       - community
 *       - academy
 *       - supply
 *     datasource-routing:
 *       order: tidb-sharded
 *       payment: tidb-sharded
 *       ai: tidb-sharded
 *       copyright: tidb-sharded
 *       community: mysql-master-slave
 *       academy: mysql-master-slave
 *       supply: mysql-master-slave
 * </pre>
 *
 * @author Tailor IS Team
 * @since 2.0.0
 */
@Data
@Component
@ConfigurationProperties(prefix = "tailoris.sharding")
public class ShardingStrategyConfig {

    /** 是否启用分片 */
    private boolean enabled = false;

    /** 分片数量（默认 4 分片） */
    private int shardCount = 4;

    /** 分片键列名（默认 merchant_id） */
    private String shardingColumn = "merchant_id";

    /** 核心服务列表（使用 TiDB + 分片） */
    private String[] coreServices = {
            "order",
            "payment",
            "ai",
            "copyright"
    };

    /** 非核心服务列表（使用 MySQL 主从） */
    private String[] nonCoreServices = {
            "community",
            "academy",
            "supply"
    };

    /** 数据源路由映射 (service-name → datasource-type) */
    private Map<String, String> datasourceRouting = new HashMap<>();

    /**
     * 数据源类型枚举
     */
    public enum DataSourceType {
        /** TiDB + ShardingSphere 分片数据源 */
        TIDB_SHARDED("tidb-sharded"),
        /** MySQL 主从数据源 */
        MYSQL_MASTER_SLAVE("mysql-master-slave"),
        /** MySQL 单实例数据源 */
        MYSQL_SINGLE("mysql-single");

        private final String value;

        DataSourceType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    /**
     * 服务分类枚举
     */
    public enum ServiceCategory {
        CORE,
        NON_CORE
    }

    public ShardingStrategyConfig() {
        // 初始化默认路由映射
        datasourceRouting.put("order", DataSourceType.TIDB_SHARDED.getValue());
        datasourceRouting.put("payment", DataSourceType.TIDB_SHARDED.getValue());
        datasourceRouting.put("ai", DataSourceType.TIDB_SHARDED.getValue());
        datasourceRouting.put("copyright", DataSourceType.TIDB_SHARDED.getValue());
        datasourceRouting.put("community", DataSourceType.MYSQL_MASTER_SLAVE.getValue());
        datasourceRouting.put("academy", DataSourceType.MYSQL_MASTER_SLAVE.getValue());
        datasourceRouting.put("supply", DataSourceType.MYSQL_MASTER_SLAVE.getValue());
    }

    /**
     * 判断指定服务是否为核心服务
     *
     * @param serviceName 服务名称
     * @return true 如果服务在核心服务列表中
     */
    public boolean isCoreService(String serviceName) {
        if (coreServices == null) {
            return false;
        }
        for (String core : coreServices) {
            if (core.equalsIgnoreCase(serviceName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取服务分类
     *
     * @param serviceName 服务名称
     * @return 服务分类 (CORE / NON_CORE)
     */
    public ServiceCategory getServiceCategory(String serviceName) {
        return isCoreService(serviceName) ? ServiceCategory.CORE : ServiceCategory.NON_CORE;
    }

    /**
     * 获取指定服务的数据源类型
     *
     * @param serviceName 服务名称
     * @return 数据源类型字符串
     */
    public String getDataSourceType(String serviceName) {
        return datasourceRouting.getOrDefault(serviceName, DataSourceType.MYSQL_SINGLE.getValue());
    }

    /**
     * 根据分片键计算分片索引
     * 公式: |hash(shardingKey)| % shardCount
     *
     * @param shardingKey 分片键值
     * @return 分片索引 (0 ~ shardCount-1)
     */
    public int calculateShardIndex(Object shardingKey) {
        if (shardingKey == null) {
            return 0;
        }
        return Math.abs(shardingKey.hashCode()) % shardCount;
    }

    /**
     * 获取分片后缀
     * 例如: shardSuffix(12345) → "_1"
     *
     * @param shardingKey 分片键值
     * @return 分片后缀字符串
     */
    public String getShardSuffix(Object shardingKey) {
        return "_" + calculateShardIndex(shardingKey);
    }
}
