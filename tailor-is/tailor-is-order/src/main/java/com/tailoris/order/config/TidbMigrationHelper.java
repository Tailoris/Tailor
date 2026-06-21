package com.tailoris.order.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * TiDB 表迁移辅助工具.
 *
 * <p>当 {@code tidb} profile 激活时，在应用启动时自动检查并创建 TiDB 表结构。
 * 通过 {@link jakarta.annotation.PostConstruct} 方法触发表初始化。</p>
 *
 * <p>TiDB 优化特性：
 *   - SHARD_ROW_ID_BITS=4: 行 ID 打散，防止热点写入
 *   - PRE_SPLIT_REGIONS=4: 预切分 Region，提前分散数据
 *   - AUTO_RANDOM: 替代 AUTO_INCREMENT，分布式场景下避免写热点
 *   - 按 create_time 进行 RANGE 分区，优化时间范围查询
 * </p>
 */
@Slf4j
@Component
@Profile("tidb")
@ConditionalOnProperty(name = "tidb.enabled", havingValue = "true", matchIfMissing = true)
public class TidbMigrationHelper {

    private final JdbcTemplate jdbcTemplate;

    private static final String[] TIDB_TABLES = {"order_info", "order_item"};

    public TidbMigrationHelper(@Qualifier("tidbDataSource") DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    /**
     * 应用启动时自动检查并创建 TiDB 表结构.
     */
    @jakarta.annotation.PostConstruct
    public void initTables() {
        log.info("TiDB migration helper: checking tables...");
        try {
            if (areAllTablesCreated()) {
                log.info("TiDB tables already exist, skipping migration.");
                return;
            }
            String schema = loadSchemaSql();
            if (schema != null && !schema.isBlank()) {
                executeSchema(schema);
                log.info("TiDB tables created successfully.");
            } else {
                log.warn("TiDB schema SQL file not found, tables will not be auto-created.");
            }
        } catch (Exception e) {
            log.error("Failed to initialize TiDB tables: {}", e.getMessage(), e);
        }
    }

    /**
     * 检查 TiDB 表是否已全部创建.
     */
    public boolean areAllTablesCreated() {
        for (String table : TIDB_TABLES) {
            try {
                List<String> result = jdbcTemplate.queryForList(
                        "SHOW TABLES LIKE ?", String.class, table);
                if (result.isEmpty()) {
                    log.info("TiDB table '{}' does not exist.", table);
                    return false;
                }
            } catch (Exception e) {
                log.warn("Failed to check TiDB table '{}': {}", table, e.getMessage());
                return false;
            }
        }
        return true;
    }

    /**
     * 从 classpath 加载 TiDB schema DDL 文件.
     */
    private String loadSchemaSql() {
        try {
            ClassPathResource resource = new ClassPathResource("db/tidb-schema.sql");
            if (!resource.exists()) {
                log.warn("TiDB schema file not found at db/tidb-schema.sql");
                return null;
            }
            return StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Failed to load TiDB schema SQL: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 执行 DDL 语句创建表.
     * <p>按分号拆分多条 SQL 语句，逐条执行。</p>
     */
    private void executeSchema(String schema) {
        String[] statements = schema.split(";");
        for (String sql : statements) {
            String trimmed = sql.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("--") || trimmed.startsWith("#")) {
                continue;
            }
            try {
                jdbcTemplate.execute(trimmed);
                log.debug("TiDB DDL executed: {}", trimmed.length() > 80
                        ? trimmed.substring(0, 80) + "..." : trimmed);
            } catch (Exception e) {
                log.error("Failed to execute TiDB DDL: {}", e.getMessage());
                throw new RuntimeException("TiDB schema migration failed", e);
            }
        }
    }
}