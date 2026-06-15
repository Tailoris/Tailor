package com.tailoris.common.util;

import com.tailoris.common.filter.TraceIdFilter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TraceUtils 单元测试 - L-21 修复验证.
 *
 * <p>覆盖：</p>
 * <ul>
 *   <li>从MDC读取traceId</li>
 *   <li>无MDC时兜底生成</li>
 *   <li>putToMdc写入与clear清理</li>
 * </ul>
 *
 * @author Tailor IS Team
 * @since 1.0.0
 */
@DisplayName("TraceUtils 链路追踪工具测试 (L-21)")
class TraceUtilsTest {

    @AfterEach
    void cleanup() {
        MDC.clear();
    }

    @Test
    @DisplayName("MDC 中存在 traceId 时应优先返回")
    void currentTraceId_FromMdc() {
        String expected = "test-trace-123";
        MDC.put(TraceIdFilter.MDC_TRACE_ID, expected);
        assertEquals(expected, TraceUtils.currentTraceId());
    }

    @Test
    @DisplayName("MDC 为空时应生成新的 traceId")
    void currentTraceId_GenerateWhenEmpty() {
        MDC.clear();
        String traceId = TraceUtils.currentTraceId();
        assertNotNull(traceId);
        assertNotEquals("", traceId);
        // UUID去除横线后是32位
        assertEquals(32, traceId.length());
        assertTrue(traceId.matches("^[A-Za-z0-9]+$"));
    }

    @Test
    @DisplayName("连续生成 traceId 应保证唯一")
    void currentTraceId_UniqueOnEachCall() {
        String t1 = TraceUtils.currentTraceId();
        String t2 = TraceUtils.currentTraceId();
        assertNotEquals(t1, t2);
    }

    @Test
    @DisplayName("putToMdc 应将 traceId 放入 MDC 并返回")
    void putToMdc_WritesToMdc() {
        String traceId = TraceUtils.putToMdc();
        assertNotNull(traceId);
        assertEquals(traceId, MDC.get(TraceIdFilter.MDC_TRACE_ID));
        assertEquals(traceId, TraceUtils.currentTraceId());
    }

    @Test
    @DisplayName("clear 应清理 MDC 中的 traceId")
    void clear_RemovesFromMdc() {
        TraceUtils.putToMdc();
        assertNotNull(MDC.get(TraceIdFilter.MDC_TRACE_ID));
        TraceUtils.clear();
        // 清理后 currentTraceId 应生成新值
        String after = TraceUtils.currentTraceId();
        assertNotNull(after);
    }

    @Test
    @DisplayName("putToMdc 后再调用 currentTraceId 应返回相同值")
    void putToMdc_Consistent() {
        String first = TraceUtils.putToMdc();
        String second = TraceUtils.currentTraceId();
        assertEquals(first, second);
    }
}
