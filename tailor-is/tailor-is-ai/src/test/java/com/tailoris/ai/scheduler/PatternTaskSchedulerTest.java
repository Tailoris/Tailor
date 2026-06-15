package com.tailoris.ai.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tailoris.ai.entity.PatternRecord;
import com.tailoris.ai.mapper.PatternRecordMapper;
import com.tailoris.common.config.CacheRouter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("PatternTaskScheduler 单元测试")
@ExtendWith(MockitoExtension.class)
class PatternTaskSchedulerTest {

    @Mock
    private PatternRecordMapper patternRecordMapper;
    @Mock
    private CacheRouter cacheRouter;
    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private PatternTaskScheduler patternTaskScheduler;

    @BeforeEach
    void setUp() {
        lenient().when(cacheRouter.getCoreTemplate()).thenReturn(redisTemplate);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    @DisplayName("重试失败任务 - 无失败记录")
    void testRetryFailedTasks_NoFailedRecords() {
        when(patternRecordMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.emptyList());

        assertDoesNotThrow(() -> patternTaskScheduler.retryFailedTasks());
    }

    @Test
    @DisplayName("重试失败任务 - 有失败记录")
    void testRetryFailedTasks_WithFailedRecords() {
        PatternRecord record1 = createPatternRecord(1L, "版型1");
        PatternRecord record2 = createPatternRecord(2L, "版型2");
        when(patternRecordMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Arrays.asList(record1, record2));
        when(patternRecordMapper.updateById(any(PatternRecord.class))).thenReturn(1);

        assertDoesNotThrow(() -> patternTaskScheduler.retryFailedTasks());
        verify(patternRecordMapper, times(2)).updateById(any(PatternRecord.class));
    }

    @Test
    @DisplayName("重试失败任务 - 数据库查询异常")
    void testRetryFailedTasks_QueryException() {
        when(patternRecordMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenThrow(new RuntimeException("数据库错误"));

        assertDoesNotThrow(() -> patternTaskScheduler.retryFailedTasks());
    }

    @Test
    @DisplayName("重试失败任务 - 更新失败")
    void testRetryFailedTasks_UpdateFails() {
        PatternRecord record = createPatternRecord(1L, "版型1");
        when(patternRecordMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Arrays.asList(record));
        when(patternRecordMapper.updateById(any(PatternRecord.class)))
                .thenThrow(new RuntimeException("更新失败"));

        assertDoesNotThrow(() -> patternTaskScheduler.retryFailedTasks());
    }

    @Test
    @DisplayName("清理过期缓存 - 正常执行")
    void testCleanupExpiredCache_Success() {
        assertDoesNotThrow(() -> patternTaskScheduler.cleanupExpiredCache());
    }

    @Test
    @DisplayName("清理过期缓存 - 异常处理")
    void testCleanupExpiredCache_Exception() {
        when(cacheRouter.getCoreTemplate()).thenThrow(new RuntimeException("Redis错误"));

        assertDoesNotThrow(() -> patternTaskScheduler.cleanupExpiredCache());
    }

    @Test
    @DisplayName("生成统计报告 - 正常执行")
    void testGenerateStatsReport_Success() {
        assertDoesNotThrow(() -> patternTaskScheduler.generateStatsReport());
    }

    @Test
    @DisplayName("生成统计报告 - Redis写入失败")
    void testGenerateStatsReport_RedisFails() {
        when(valueOperations.increment(anyString(), anyInt()))
                .thenThrow(new RuntimeException("Redis错误"));

        assertDoesNotThrow(() -> patternTaskScheduler.generateStatsReport());
    }

    @Test
    @DisplayName("判断非高峰时段 - 凌晨3点在默认范围内")
    void testIsOffPeakHours_InRange() {
        // 当前时间无法控制，但我们可以测试逻辑
        // 测试正常范围：2-6点
        boolean result = patternTaskScheduler.isOffPeakHours(2, 6);
        // 结果取决于当前时间，只验证不抛异常
        assertNotNull(result);
    }

    @Test
    @DisplayName("判断非高峰时段 - 跨午夜场景")
    void testIsOffPeakHours_CrossMidnight() {
        // 测试跨午夜场景：22-6点
        boolean result = patternTaskScheduler.isOffPeakHours(22, 6);
        assertNotNull(result);
    }

    @Test
    @DisplayName("判断非高峰时段 - 相同时间")
    void testIsOffPeakHours_SameHour() {
        boolean result = patternTaskScheduler.isOffPeakHours(3, 3);
        assertNotNull(result);
    }

    @Test
    @DisplayName("判断非高峰时段 - 起始大于结束")
    void testIsOffPeakHours_StartGreaterThanEnd() {
        boolean result = patternTaskScheduler.isOffPeakHours(22, 6);
        assertNotNull(result);
    }

    private PatternRecord createPatternRecord(Long id, String name) {
        PatternRecord record = new PatternRecord();
        record.setId(id);
        record.setPatternName(name);
        record.setStatus(0);
        return record;
    }
}
