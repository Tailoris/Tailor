package com.tailoris.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("N1QueryFixPatterns 测试")
class N1QueryFixPatternsTest {

    public static class Source {
        private Long id;
        private Long relatedId;
        private Object related;

        public Source() {}

        public Source(Long id, Long relatedId) {
            this.id = id;
            this.relatedId = relatedId;
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public Long getRelatedId() { return relatedId; }
        public void setRelatedId(Long relatedId) { this.relatedId = relatedId; }
        public Object getRelated() { return related; }
        public void setRelated(Object related) { this.related = related; }
    }

    public static class Related {
        private Long id;
        private String name;

        public Related() {}

        public Related(Long id, String name) {
            this.id = id;
            this.name = name;
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }

    @Nested
    @DisplayName("batchAssignFromList 测试")
    class BatchAssignTests {

        @Test
        @DisplayName("批量赋值 - 正常情况")
        void testBatchAssignNormal() {
            List<Source> sourceList = Arrays.asList(
                new Source(1L, 10L),
                new Source(2L, 20L),
                new Source(3L, 10L)
            );

            List<Related> relatedList = Arrays.asList(
                new Related(10L, "Related10"),
                new Related(20L, "Related20")
            );

            N1QueryFixPatterns.batchAssignFromList(
                sourceList,
                Source::getRelatedId,
                relatedList,
                Related::getId,
                Source::setRelated
            );

            assertEquals("Related10", ((Related) sourceList.get(0).getRelated()).getName());
            assertEquals("Related20", ((Related) sourceList.get(1).getRelated()).getName());
            assertEquals("Related10", ((Related) sourceList.get(2).getRelated()).getName());
        }

        @Test
        @DisplayName("批量赋值 - 空源列表")
        void testBatchAssignEmptySource() {
            List<Source> sourceList = new ArrayList<>();
            List<Related> relatedList = Arrays.asList(new Related(10L, "Test"));

            assertDoesNotThrow(() -> {
                N1QueryFixPatterns.batchAssignFromList(
                    sourceList,
                    Source::getRelatedId,
                    relatedList,
                    Related::getId,
                    Source::setRelated
                );
            });
        }

        @Test
        @DisplayName("批量赋值 - null 源列表")
        void testBatchAssignNullSource() {
            List<Related> relatedList = Arrays.asList(new Related(10L, "Test"));

            assertDoesNotThrow(() -> {
                N1QueryFixPatterns.batchAssignFromList(
                    null,
                    Source::getRelatedId,
                    relatedList,
                    Related::getId,
                    Source::setRelated
                );
            });
        }

        @Test
        @DisplayName("批量赋值 - 空关联列表")
        void testBatchAssignEmptyRelated() {
            List<Source> sourceList = Arrays.asList(new Source(1L, 10L));
            List<Related> relatedList = new ArrayList<>();

            N1QueryFixPatterns.batchAssignFromList(
                sourceList,
                Source::getRelatedId,
                relatedList,
                Related::getId,
                Source::setRelated
            );

            assertNull(sourceList.get(0).getRelated());
        }

        @Test
        @DisplayName("批量赋值 - 关联 ID 为 null")
        void testBatchAssignNullRelatedId() {
            List<Source> sourceList = Arrays.asList(new Source(1L, null));
            List<Related> relatedList = Arrays.asList(new Related(10L, "Test"));

            N1QueryFixPatterns.batchAssignFromList(
                sourceList,
                Source::getRelatedId,
                relatedList,
                Related::getId,
                Source::setRelated
            );

            assertNull(sourceList.get(0).getRelated());
        }

        @Test
        @DisplayName("批量赋值 - 重复关联实体取第一个")
        void testBatchAssignDuplicateRelated() {
            List<Source> sourceList = Arrays.asList(new Source(1L, 10L));
            List<Related> relatedList = Arrays.asList(
                new Related(10L, "First"),
                new Related(10L, "Second")
            );

            N1QueryFixPatterns.batchAssignFromList(
                sourceList,
                Source::getRelatedId,
                relatedList,
                Related::getId,
                Source::setRelated
            );

            assertEquals("First", ((Related) sourceList.get(0).getRelated()).getName());
        }
    }

    @Nested
    @DisplayName("常量测试")
    class ConstantTests {

        @Test
        @DisplayName("业务场景常量")
        void testScenarioConstants() {
            assertNotNull(N1QueryFixPatterns.SCENARIO_ORDER_PRODUCT);
            assertNotNull(N1QueryFixPatterns.SCENARIO_PRODUCT_SKU);
            assertNotNull(N1QueryFixPatterns.SCENARIO_POST_COMMENT);
            assertNotNull(N1QueryFixPatterns.SCENARIO_COPYRIGHT_AUTHOR);
            assertNotNull(N1QueryFixPatterns.SCENARIO_PROMOTION_PRODUCT);
        }

        @Test
        @DisplayName("修复效果指标")
        void testFixMetrics() {
            Map<String, String> metrics = N1QueryFixPatterns.FIX_METRICS;
            assertNotNull(metrics);
            assertFalse(metrics.isEmpty());
            assertTrue(metrics.containsKey("查询次数减少"));
            assertTrue(metrics.containsKey("P95响应时间减少"));
        }
    }

    @Nested
    @DisplayName("内部类测试")
    class InnerClassTests {

        @Test
        @DisplayName("BatchPrefetchPattern 类存在")
        void testBatchPrefetchPattern() {
            assertNotNull(N1QueryFixPatterns.BatchPrefetchPattern.class);
        }

        @Test
        @DisplayName("JoinQueryPattern 类存在")
        void testJoinQueryPattern() {
            assertNotNull(N1QueryFixPatterns.JoinQueryPattern.class);
        }

        @Test
        @DisplayName("CachePattern 类存在")
        void testCachePattern() {
            assertNotNull(N1QueryFixPatterns.CachePattern.class);
        }
    }
}
