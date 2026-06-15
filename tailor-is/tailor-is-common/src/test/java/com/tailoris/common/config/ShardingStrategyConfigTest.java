package com.tailoris.common.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ShardingStrategyConfig 测试")
class ShardingStrategyConfigTest {

    @Nested
    @DisplayName("默认值测试")
    class DefaultValueTests {

        @Test
        @DisplayName("默认不启用")
        void testDefaultEnabled() {
            ShardingStrategyConfig config = new ShardingStrategyConfig();
            assertFalse(config.isEnabled());
        }

        @Test
        @DisplayName("默认分片数量")
        void testDefaultShardCount() {
            ShardingStrategyConfig config = new ShardingStrategyConfig();
            assertEquals(4, config.getShardCount());
        }

        @Test
        @DisplayName("默认分片列")
        void testDefaultShardingColumn() {
            ShardingStrategyConfig config = new ShardingStrategyConfig();
            assertEquals("merchant_id", config.getShardingColumn());
        }

        @Test
        @DisplayName("默认核心服务")
        void testDefaultCoreServices() {
            ShardingStrategyConfig config = new ShardingStrategyConfig();
            assertNotNull(config.getCoreServices());
            assertEquals(4, config.getCoreServices().length);
        }

        @Test
        @DisplayName("默认非核心服务")
        void testDefaultNonCoreServices() {
            ShardingStrategyConfig config = new ShardingStrategyConfig();
            assertNotNull(config.getNonCoreServices());
            assertEquals(3, config.getNonCoreServices().length);
        }

        @Test
        @DisplayName("默认数据源路由")
        void testDefaultDatasourceRouting() {
            ShardingStrategyConfig config = new ShardingStrategyConfig();
            assertNotNull(config.getDatasourceRouting());
            assertFalse(config.getDatasourceRouting().isEmpty());
        }
    }

    @Nested
    @DisplayName("isCoreService 测试")
    class IsCoreServiceTests {

        @Test
        @DisplayName("核心服务返回 true")
        void testIsCoreServiceTrue() {
            ShardingStrategyConfig config = new ShardingStrategyConfig();
            assertTrue(config.isCoreService("order"));
            assertTrue(config.isCoreService("payment"));
            assertTrue(config.isCoreService("ai"));
            assertTrue(config.isCoreService("copyright"));
        }

        @Test
        @DisplayName("非核心服务返回 false")
        void testIsCoreServiceFalse() {
            ShardingStrategyConfig config = new ShardingStrategyConfig();
            assertFalse(config.isCoreService("community"));
            assertFalse(config.isCoreService("academy"));
            assertFalse(config.isCoreService("supply"));
        }

        @Test
        @DisplayName("大小写不敏感")
        void testIsCoreServiceCaseInsensitive() {
            ShardingStrategyConfig config = new ShardingStrategyConfig();
            assertTrue(config.isCoreService("ORDER"));
            assertTrue(config.isCoreService("Order"));
        }

        @Test
        @DisplayName("null 返回 false")
        void testIsCoreServiceNull() {
            ShardingStrategyConfig config = new ShardingStrategyConfig();
            assertFalse(config.isCoreService(null));
        }

        @Test
        @DisplayName("coreServices 为 null 返回 false")
        void testIsCoreServiceNullArray() {
            ShardingStrategyConfig config = new ShardingStrategyConfig();
            config.setCoreServices(null);
            assertFalse(config.isCoreService("order"));
        }
    }

    @Nested
    @DisplayName("getServiceCategory 测试")
    class GetServiceCategoryTests {

        @Test
        @DisplayName("核心服务返回 CORE")
        void testGetServiceCategoryCore() {
            ShardingStrategyConfig config = new ShardingStrategyConfig();
            assertEquals(ShardingStrategyConfig.ServiceCategory.CORE, config.getServiceCategory("order"));
        }

        @Test
        @DisplayName("非核心服务返回 NON_CORE")
        void testGetServiceCategoryNonCore() {
            ShardingStrategyConfig config = new ShardingStrategyConfig();
            assertEquals(ShardingStrategyConfig.ServiceCategory.NON_CORE, config.getServiceCategory("community"));
        }
    }

    @Nested
    @DisplayName("getDataSourceType 测试")
    class GetDataSourceTypeTests {

        @Test
        @DisplayName("已配置服务返回对应数据源")
        void testGetDataSourceTypeConfigured() {
            ShardingStrategyConfig config = new ShardingStrategyConfig();
            assertEquals("tidb-sharded", config.getDataSourceType("order"));
            assertEquals("mysql-master-slave", config.getDataSourceType("community"));
        }

        @Test
        @DisplayName("未配置服务返回默认数据源")
        void testGetDataSourceTypeDefault() {
            ShardingStrategyConfig config = new ShardingStrategyConfig();
            assertEquals("mysql-single", config.getDataSourceType("unknown"));
        }
    }

    @Nested
    @DisplayName("calculateShardIndex 测试")
    class CalculateShardIndexTests {

        @Test
        @DisplayName("null 返回 0")
        void testCalculateShardIndexNull() {
            ShardingStrategyConfig config = new ShardingStrategyConfig();
            assertEquals(0, config.calculateShardIndex(null));
        }

        @Test
        @DisplayName("正常计算分片索引")
        void testCalculateShardIndexNormal() {
            ShardingStrategyConfig config = new ShardingStrategyConfig();
            int index = config.calculateShardIndex("test");
            assertTrue(index >= 0 && index < 4);
        }

        @Test
        @DisplayName("相同键返回相同索引")
        void testCalculateShardIndexConsistent() {
            ShardingStrategyConfig config = new ShardingStrategyConfig();
            int index1 = config.calculateShardIndex("test");
            int index2 = config.calculateShardIndex("test");
            assertEquals(index1, index2);
        }

        @Test
        @DisplayName("不同 shardCount 计算")
        void testCalculateShardIndexDifferentCount() {
            ShardingStrategyConfig config = new ShardingStrategyConfig();
            config.setShardCount(8);
            int index = config.calculateShardIndex("test");
            assertTrue(index >= 0 && index < 8);
        }
    }

    @Nested
    @DisplayName("getShardSuffix 测试")
    class GetShardSuffixTests {

        @Test
        @DisplayName("获取分片后缀")
        void testGetShardSuffix() {
            ShardingStrategyConfig config = new ShardingStrategyConfig();
            String suffix = config.getShardSuffix("test");
            assertNotNull(suffix);
            assertTrue(suffix.startsWith("_"));
        }

        @Test
        @DisplayName("null 键返回 _0")
        void testGetShardSuffixNull() {
            ShardingStrategyConfig config = new ShardingStrategyConfig();
            assertEquals("_0", config.getShardSuffix(null));
        }
    }

    @Nested
    @DisplayName("枚举测试")
    class EnumTests {

        @Test
        @DisplayName("DataSourceType 枚举值")
        void testDataSourceTypeEnum() {
            assertEquals("tidb-sharded", ShardingStrategyConfig.DataSourceType.TIDB_SHARDED.getValue());
            assertEquals("mysql-master-slave", ShardingStrategyConfig.DataSourceType.MYSQL_MASTER_SLAVE.getValue());
            assertEquals("mysql-single", ShardingStrategyConfig.DataSourceType.MYSQL_SINGLE.getValue());
        }

        @Test
        @DisplayName("ServiceCategory 枚举值")
        void testServiceCategoryEnum() {
            assertNotNull(ShardingStrategyConfig.ServiceCategory.CORE);
            assertNotNull(ShardingStrategyConfig.ServiceCategory.NON_CORE);
        }
    }

    @Nested
    @DisplayName("Setter 测试")
    class SetterTests {

        @Test
        @DisplayName("设置 enabled")
        void testSetEnabled() {
            ShardingStrategyConfig config = new ShardingStrategyConfig();
            config.setEnabled(true);
            assertTrue(config.isEnabled());
        }

        @Test
        @DisplayName("设置 shardCount")
        void testSetShardCount() {
            ShardingStrategyConfig config = new ShardingStrategyConfig();
            config.setShardCount(8);
            assertEquals(8, config.getShardCount());
        }

        @Test
        @DisplayName("设置 shardingColumn")
        void testSetShardingColumn() {
            ShardingStrategyConfig config = new ShardingStrategyConfig();
            config.setShardingColumn("user_id");
            assertEquals("user_id", config.getShardingColumn());
        }
    }
}
