package com.tailoris.common.sync;

import com.tailoris.common.config.DataSyncStrategyConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("DataConsistencyValidator 测试")
class DataConsistencyValidatorTest {

    private DataConsistencyValidator validator;

    @Nested
    @DisplayName("CheckResult 测试")
    class CheckResultTests {

        @Test
        @DisplayName("创建成功结果")
        void testCheckResultOk() {
            DataConsistencyValidator.CheckResult result = DataConsistencyValidator.CheckResult.ok(10);
            assertTrue(result.consistent());
            assertEquals(10, result.totalChecked());
            assertEquals(0, result.discrepancies());
            assertTrue(result.discrepancyIds().isEmpty());
            assertEquals("一致", result.details());
        }

        @Test
        @DisplayName("创建失败结果")
        void testCheckResultFail() {
            List<String> ids = Arrays.asList("id1", "id2");
            DataConsistencyValidator.CheckResult result = DataConsistencyValidator.CheckResult.fail(10, ids, "不一致");
            assertFalse(result.consistent());
            assertEquals(10, result.totalChecked());
            assertEquals(2, result.discrepancies());
            assertEquals(ids, result.discrepancyIds());
            assertEquals("不一致", result.details());
        }
    }

    @Nested
    @DisplayName("validateAll 测试")
    class ValidateAllTests {

        @Test
        @DisplayName("无校验器时跳过")
        void testValidateAllNoCheckers() {
            validator = new DataConsistencyValidator(Collections.emptyList());
            assertDoesNotThrow(() -> validator.validateAll());
        }

        @Test
        @DisplayName("null 校验器列表跳过")
        void testValidateAllNullCheckers() {
            validator = new DataConsistencyValidator(null);
            assertDoesNotThrow(() -> validator.validateAll());
        }

        @Test
        @DisplayName("执行校验 - 一致")
        void testValidateAllConsistent() {
            DataConsistencyValidator.ConsistencyChecker checker = mock(DataConsistencyValidator.ConsistencyChecker.class);
            when(checker.getDataType()).thenReturn("order");
            when(checker.check()).thenReturn(DataConsistencyValidator.CheckResult.ok(10));

            validator = new DataConsistencyValidator(List.of(checker));
            validator.validateAll();

            List<DataConsistencyValidator.ValidationReport> reports = validator.getRecentReports(10);
            assertEquals(1, reports.size());
            assertTrue(reports.get(0).consistent());
        }

        @Test
        @DisplayName("执行校验 - 不一致且非核心数据自动修复")
        void testValidateAllInconsistentNonCoreAutoRepair() {
            DataConsistencyValidator.ConsistencyChecker checker = mock(DataConsistencyValidator.ConsistencyChecker.class);
            when(checker.getDataType()).thenReturn("community_post");
            when(checker.check()).thenReturn(
                DataConsistencyValidator.CheckResult.fail(10, List.of("id1", "id2"), "不一致")
            );
            when(checker.autoRepair("id1")).thenReturn(true);
            when(checker.autoRepair("id2")).thenReturn(false);

            validator = new DataConsistencyValidator(List.of(checker));
            validator.validateAll();

            List<DataConsistencyValidator.ValidationReport> reports = validator.getRecentReports(10);
            assertEquals(1, reports.size());
            assertFalse(reports.get(0).consistent());
            assertTrue(reports.get(0).autoRepairAttempted());
            assertEquals(1, reports.get(0).autoRepairSuccess());
        }

        @Test
        @DisplayName("执行校验 - 不一致且核心数据不自动修复")
        void testValidateAllInconsistentCoreNoAutoRepair() {
            DataConsistencyValidator.ConsistencyChecker checker = mock(DataConsistencyValidator.ConsistencyChecker.class);
            when(checker.getDataType()).thenReturn("order");
            when(checker.check()).thenReturn(
                DataConsistencyValidator.CheckResult.fail(10, List.of("id1"), "不一致")
            );

            validator = new DataConsistencyValidator(List.of(checker));
            validator.validateAll();

            List<DataConsistencyValidator.ValidationReport> reports = validator.getRecentReports(10);
            assertEquals(1, reports.size());
            assertFalse(reports.get(0).consistent());
            assertFalse(reports.get(0).autoRepairAttempted());
        }

        @Test
        @DisplayName("校验器异常时记录失败报告")
        void testValidateAllCheckerException() {
            DataConsistencyValidator.ConsistencyChecker checker = mock(DataConsistencyValidator.ConsistencyChecker.class);
            when(checker.getDataType()).thenReturn("order");
            when(checker.check()).thenThrow(new RuntimeException("校验异常"));

            validator = new DataConsistencyValidator(List.of(checker));
            validator.validateAll();

            List<DataConsistencyValidator.ValidationReport> reports = validator.getRecentReports(10);
            assertEquals(1, reports.size());
            assertFalse(reports.get(0).consistent());
        }
    }

    @Nested
    @DisplayName("getRecentReports 测试")
    class GetRecentReportsTests {

        @Test
        @DisplayName("获取最近报告")
        void testGetRecentReports() {
            DataConsistencyValidator.ConsistencyChecker checker = mock(DataConsistencyValidator.ConsistencyChecker.class);
            when(checker.getDataType()).thenReturn("order");
            when(checker.check()).thenReturn(DataConsistencyValidator.CheckResult.ok(10));

            validator = new DataConsistencyValidator(List.of(checker));
            validator.validateAll();

            List<DataConsistencyValidator.ValidationReport> reports = validator.getRecentReports(10);
            assertFalse(reports.isEmpty());
        }

        @Test
        @DisplayName("限制返回数量")
        void testGetRecentReportsLimit() {
            validator = new DataConsistencyValidator(Collections.emptyList());
            List<DataConsistencyValidator.ValidationReport> reports = validator.getRecentReports(5);
            assertTrue(reports.isEmpty());
        }
    }

    @Nested
    @DisplayName("getStats 测试")
    class GetStatsTests {

        @Test
        @DisplayName("获取统计信息")
        void testGetStats() {
            DataConsistencyValidator.ConsistencyChecker checker = mock(DataConsistencyValidator.ConsistencyChecker.class);
            when(checker.getDataType()).thenReturn("order");
            when(checker.check()).thenReturn(DataConsistencyValidator.CheckResult.ok(10));

            validator = new DataConsistencyValidator(List.of(checker));
            validator.validateAll();

            Map<String, Object> stats = validator.getStats();
            assertNotNull(stats);
            assertTrue(stats.containsKey("totalValidations"));
            assertTrue(stats.containsKey("totalDiscrepancies"));
            assertTrue(stats.containsKey("totalAutoRepairs"));
            assertTrue(stats.containsKey("recentReportCount"));
            assertEquals(1, stats.get("totalValidations"));
        }
    }

    @Nested
    @DisplayName("validateNow 测试")
    class ValidateNowTests {

        @Test
        @DisplayName("手动触发指定数据类型校验")
        void testValidateNow() {
            DataConsistencyValidator.ConsistencyChecker checker = mock(DataConsistencyValidator.ConsistencyChecker.class);
            when(checker.getDataType()).thenReturn("order");
            when(checker.check()).thenReturn(DataConsistencyValidator.CheckResult.ok(10));

            validator = new DataConsistencyValidator(List.of(checker));
            DataConsistencyValidator.ValidationReport report = validator.validateNow("order");

            assertNotNull(report);
            assertTrue(report.consistent());
        }

        @Test
        @DisplayName("未找到校验器返回 null")
        void testValidateNowNotFound() {
            DataConsistencyValidator.ConsistencyChecker checker = mock(DataConsistencyValidator.ConsistencyChecker.class);
            when(checker.getDataType()).thenReturn("order");

            validator = new DataConsistencyValidator(List.of(checker));
            DataConsistencyValidator.ValidationReport report = validator.validateNow("nonexistent");

            assertNull(report);
        }
    }

    @Nested
    @DisplayName("报告数量限制测试")
    class ReportLimitTests {

        @Test
        @DisplayName("报告数量超过 100 时只保留最新 100 条")
        void testReportLimit() {
            DataConsistencyValidator.ConsistencyChecker checker = mock(DataConsistencyValidator.ConsistencyChecker.class);
            when(checker.getDataType()).thenReturn("order");
            when(checker.check()).thenReturn(DataConsistencyValidator.CheckResult.ok(10));

            validator = new DataConsistencyValidator(List.of(checker));

            // 执行 105 次校验
            for (int i = 0; i < 105; i++) {
                validator.validateAll();
            }

            List<DataConsistencyValidator.ValidationReport> reports = validator.getRecentReports(200);
            assertEquals(100, reports.size());
        }
    }
}
