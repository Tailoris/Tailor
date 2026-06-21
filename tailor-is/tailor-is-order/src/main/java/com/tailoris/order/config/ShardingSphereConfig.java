package com.tailoris.order.config;

import com.alibaba.druid.pool.DruidDataSource;
import lombok.extern.slf4j.Slf4j;
import org.apache.shardingsphere.driver.api.ShardingSphereDataSourceFactory;
import org.apache.shardingsphere.infra.algorithm.core.config.AlgorithmConfiguration;
import org.apache.shardingsphere.sharding.api.config.ShardingRuleConfiguration;
import org.apache.shardingsphere.sharding.api.config.rule.ShardingTableRuleConfiguration;
import org.apache.shardingsphere.sharding.api.config.strategy.keygen.KeyGenerateStrategyConfiguration;
import org.apache.shardingsphere.sharding.api.config.strategy.sharding.StandardShardingStrategyConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * ShardingSphere-JDBC 分片数据源配置.
 *
 * <p>当 {@code sharding} profile 激活时，通过 ShardingSphere 接管数据源，
 * 按 {@code merchant_id} 进行分片，将订单表拆分为 4 个分片表。</p>
 *
 * <p>分片策略：
 *   - 分片键: merchant_id
 *   - 分片算法: INLINE (merchant_id.hashCode() % 4)
 *   - 分布式主键: Snowflake
 * </p>
 *
 * <p>包含健康检查端点，用于监控分片数据源连接状态。</p>
 */
@Slf4j
@Configuration
@Profile("sharding")
@ConditionalOnProperty(name = "sharding.enabled", havingValue = "true", matchIfMissing = true)
public class ShardingSphereConfig {

    @Value("${spring.datasource.url}")
    private String dbUrl;

    @Value("${spring.datasource.username}")
    private String dbUsername;

    @Value("${spring.datasource.password}")
    private String dbPassword;

    @Value("${SHARDING_WORKER_ID:0}")
    private long workerId;

    /**
     * 创建 ShardingSphere 数据源，标记为 @Primary 以替代默认数据源.
     */
    @Primary
    @Bean(name = "shardingDataSource")
    public DataSource shardingDataSource() throws SQLException {
        // 底层物理数据源
        Map<String, DataSource> dataSourceMap = new HashMap<>(1);
        dataSourceMap.put("ds0", createDruidDataSource());

        // 分片表规则
        ShardingRuleConfiguration shardingRuleConfig = createShardingRuleConfiguration();

        // 属性配置
        Properties props = new Properties();
        props.setProperty("sql-show", "true");
        props.setProperty("check-table-metadata-enabled", "true");

        DataSource dataSource = ShardingSphereDataSourceFactory.createDataSource(
                dataSourceMap,
                Collections.singletonList(shardingRuleConfig),
                props);

        log.info("ShardingSphere DataSource initialized successfully, workerId={}", workerId);
        return dataSource;
    }

    /**
     * 健康检查端点 - 验证分片数据源连接状态.
     */
    @Bean(name = "shardingHealthCheck")
    public java.util.function.Supplier<Map<String, Object>> shardingHealthCheck() {
        return () -> {
            Map<String, Object> status = new HashMap<>();
            status.put("sharding", "enabled");
            status.put("workerId", workerId);
            try (var conn = shardingDataSource().getConnection()) {
                if (conn.isValid(3)) {
                    status.put("connection", "healthy");
                } else {
                    status.put("connection", "unhealthy");
                }
            } catch (Exception e) {
                status.put("connection", "error");
                status.put("error", e.getMessage());
            }
            return status;
        };
    }

    /**
     * 创建 Druid 物理数据源.
     */
    private DruidDataSource createDruidDataSource() {
        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
        dataSource.setUrl(dbUrl);
        dataSource.setUsername(dbUsername);
        dataSource.setPassword(dbPassword);
        dataSource.setInitialSize(5);
        dataSource.setMinIdle(5);
        dataSource.setMaxActive(20);
        dataSource.setMaxWait(60000);
        dataSource.setTimeBetweenEvictionRunsMillis(60000);
        dataSource.setMinEvictableIdleTimeMillis(300000);
        dataSource.setValidationQuery("SELECT 1");
        return dataSource;
    }

    /**
     * 创建 ShardingSphere 分片规则配置.
     */
    private ShardingRuleConfiguration createShardingRuleConfiguration() {
        ShardingRuleConfiguration shardingRuleConfig = new ShardingRuleConfiguration();

        // ========== 分片表: order_info ==========
        ShardingTableRuleConfiguration orderInfoTableRule =
                new ShardingTableRuleConfiguration("order_info", "ds0.t_order_${0..3}");
        orderInfoTableRule.setTableShardingStrategy(
                new StandardShardingStrategyConfiguration("merchant_id", "order-inline"));
        orderInfoTableRule.setKeyGenerateStrategy(
                new KeyGenerateStrategyConfiguration("id", "snowflake"));
        shardingRuleConfig.getTables().add(orderInfoTableRule);

        // ========== 分片表: order_item ==========
        ShardingTableRuleConfiguration orderItemTableRule =
                new ShardingTableRuleConfiguration("order_item", "ds0.t_order_item_${0..3}");
        orderItemTableRule.setTableShardingStrategy(
                new StandardShardingStrategyConfiguration("merchant_id", "order-item-inline"));
        orderItemTableRule.setKeyGenerateStrategy(
                new KeyGenerateStrategyConfiguration("id", "snowflake"));
        shardingRuleConfig.getTables().add(orderItemTableRule);

        // ========== 分片表: shopping_cart ==========
        ShardingTableRuleConfiguration cartTableRule =
                new ShardingTableRuleConfiguration("shopping_cart", "ds0.t_shopping_cart_${0..3}");
        cartTableRule.setTableShardingStrategy(
                new StandardShardingStrategyConfiguration("merchant_id", "cart-inline"));
        cartTableRule.setKeyGenerateStrategy(
                new KeyGenerateStrategyConfiguration("id", "snowflake"));
        shardingRuleConfig.getTables().add(cartTableRule);

        // ========== 分片表: after_sale_ticket ==========
        ShardingTableRuleConfiguration afterSaleTableRule =
                new ShardingTableRuleConfiguration("after_sale_ticket", "ds0.t_after_sale_ticket_${0..3}");
        afterSaleTableRule.setTableShardingStrategy(
                new StandardShardingStrategyConfiguration("merchant_id", "after-sale-inline"));
        afterSaleTableRule.setKeyGenerateStrategy(
                new KeyGenerateStrategyConfiguration("id", "snowflake"));
        shardingRuleConfig.getTables().add(afterSaleTableRule);

        // ========== 分片算法 ==========
        Properties inlineProps = new Properties();
        inlineProps.setProperty("algorithm-expression",
                "t_order_${Math.abs(merchant_id.hashCode()) % 4}");
        shardingRuleConfig.getShardingAlgorithms().put("order-inline",
                new AlgorithmConfiguration("INLINE", inlineProps));

        Properties orderItemInlineProps = new Properties();
        orderItemInlineProps.setProperty("algorithm-expression",
                "t_order_item_${Math.abs(merchant_id.hashCode()) % 4}");
        shardingRuleConfig.getShardingAlgorithms().put("order-item-inline",
                new AlgorithmConfiguration("INLINE", orderItemInlineProps));

        Properties cartInlineProps = new Properties();
        cartInlineProps.setProperty("algorithm-expression",
                "t_shopping_cart_${Math.abs(merchant_id.hashCode()) % 4}");
        shardingRuleConfig.getShardingAlgorithms().put("cart-inline",
                new AlgorithmConfiguration("INLINE", cartInlineProps));

        Properties afterSaleInlineProps = new Properties();
        afterSaleInlineProps.setProperty("algorithm-expression",
                "t_after_sale_ticket_${Math.abs(merchant_id.hashCode()) % 4}");
        shardingRuleConfig.getShardingAlgorithms().put("after-sale-inline",
                new AlgorithmConfiguration("INLINE", afterSaleInlineProps));

        // ========== 分布式序列: Snowflake ==========
        Properties snowflakeProps = new Properties();
        snowflakeProps.setProperty("worker-id", String.valueOf(workerId));
        shardingRuleConfig.getKeyGenerators().put("snowflake",
                new AlgorithmConfiguration("SNOWFLAKE", snowflakeProps));

        return shardingRuleConfig;
    }
}