package com.tailoris.ai.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tailoris.ai.config.CloudModelConfig;
import com.tailoris.ai.dto.PatternGenerateRequest;
import com.tailoris.ai.dto.PatternGenerateResponse;
import com.tailoris.ai.entity.PatternRecord;
import com.tailoris.ai.mapper.PatternRecordMapper;
import com.tailoris.ai.service.PatternGenerationStrategy;
import com.tailoris.common.config.CacheRouter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("OffPeakBatchGenerator 单元测试")
@ExtendWith(MockitoExtension.class)
class OffPeakBatchGeneratorTest {

    @Mock
    private PatternGenerationStrategy patternGenerationStrategy;
    @Mock
    private PatternRecordMapper patternRecordMapper;
    @Mock
    private CloudModelConfig cloudModelConfig;
    @Mock
    private CacheRouter cacheRouter;
    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Spy
    @InjectMocks
    private OffPeakBatchGenerator offPeakBatchGenerator;

    @BeforeEach
    void setUp() {
        lenient().when(cacheRouter.getCoreTemplate()).thenReturn(redisTemplate);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    @DisplayName("执行批量生成 - 空列表")
    void testExecuteBatchGeneration_EmptyList() {
        when(patternRecordMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.emptyList());

        assertDoesNotThrow(() -> offPeakBatchGenerator.executeBatchGeneration());
    }

    @Test
    @DisplayName("执行批量生成 - 有数据但生成失败")
    void testExecuteBatchGeneration_GenerationFails() {
        PatternRecord record = createPatternRecord(1L, "测试版型", 1);
        when(patternRecordMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Arrays.asList(record));
        when(patternGenerationStrategy.generatePattern(any(PatternGenerateRequest.class)))
                .thenThrow(new RuntimeException("生成失败"));

        assertDoesNotThrow(() -> offPeakBatchGenerator.executeBatchGeneration());
    }

    @Test
    @DisplayName("执行批量生成 - 成功生成并缓存")
    void testExecuteBatchGeneration_Success() {
        PatternRecord record = createPatternRecord(1L, "测试版型", 1);
        when(patternRecordMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Arrays.asList(record));

        PatternGenerateResponse response = PatternGenerateResponse.builder()
                .patternId("PAT-123")
                .name("测试版型")
                .build();
        when(patternGenerationStrategy.generatePattern(any(PatternGenerateRequest.class)))
                .thenReturn(response);

        assertDoesNotThrow(() -> offPeakBatchGenerator.executeBatchGeneration());
        verify(valueOperations, atLeastOnce()).set(anyString(), any(), any());
    }

    @Test
    @DisplayName("执行批量生成 - 响应为null")
    void testExecuteBatchGeneration_NullResponse() {
        PatternRecord record = createPatternRecord(1L, "测试版型", 1);
        when(patternRecordMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Arrays.asList(record));
        when(patternGenerationStrategy.generatePattern(any(PatternGenerateRequest.class)))
                .thenReturn(null);

        assertDoesNotThrow(() -> offPeakBatchGenerator.executeBatchGeneration());
    }

    @Test
    @DisplayName("执行批量生成 - 响应patternId为null")
    void testExecuteBatchGeneration_NullPatternId() {
        PatternRecord record = createPatternRecord(1L, "测试版型", 1);
        when(patternRecordMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Arrays.asList(record));

        PatternGenerateResponse response = PatternGenerateResponse.builder()
                .patternId(null)
                .build();
        when(patternGenerationStrategy.generatePattern(any(PatternGenerateRequest.class)))
                .thenReturn(response);

        assertDoesNotThrow(() -> offPeakBatchGenerator.executeBatchGeneration());
    }

    @Test
    @DisplayName("获取缓存纸样 - 缓存命中")
    void testGetCachedPattern_Hit() {
        PatternGenerateResponse response = PatternGenerateResponse.builder()
                .patternId("PAT-123")
                .build();
        when(valueOperations.get(anyString())).thenReturn(response);

        PatternGenerateResponse result = offPeakBatchGenerator.getCachedPattern("DRESS", 1L);

        assertNotNull(result);
        assertEquals("PAT-123", result.getPatternId());
    }

    @Test
    @DisplayName("获取缓存纸样 - 缓存未命中")
    void testGetCachedPattern_Miss() {
        when(valueOperations.get(anyString())).thenReturn(null);

        PatternGenerateResponse result = offPeakBatchGenerator.getCachedPattern("DRESS", 1L);

        assertNull(result);
    }

    @Test
    @DisplayName("获取缓存纸样 - 类型不匹配")
    void testGetCachedPattern_WrongType() {
        when(valueOperations.get(anyString())).thenReturn("not a response");

        PatternGenerateResponse result = offPeakBatchGenerator.getCachedPattern("DRESS", 1L);

        assertNull(result);
    }

    @Test
    @DisplayName("获取缓存纸样 - 异常处理")
    void testGetCachedPattern_Exception() {
        when(valueOperations.get(anyString())).thenThrow(new RuntimeException("Redis错误"));

        PatternGenerateResponse result = offPeakBatchGenerator.getCachedPattern("DRESS", 1L);

        assertNull(result);
    }

    @Test
    @DisplayName("BatchResult - 计数器功能")
    void testBatchResult() {
        OffPeakBatchGenerator.BatchResult result = new OffPeakBatchGenerator.BatchResult();

        assertEquals(0, result.getGenerated());
        assertEquals(0, result.getCached());

        result.incrementGenerated();
        assertEquals(1, result.getGenerated());

        result.incrementCached();
        assertEquals(1, result.getCached());

        result.incrementGenerated();
        result.incrementCached();
        assertEquals(2, result.getGenerated());
        assertEquals(2, result.getCached());
    }

    @Test
    @DisplayName("执行批量生成 - 达到批次上限")
    void testExecuteBatchGeneration_BatchLimit() {
        List<PatternRecord> records = Arrays.asList(
                createPatternRecord(1L, "版型1", 1),
                createPatternRecord(2L, "版型2", 1),
                createPatternRecord(3L, "版型3", 1)
        );
        when(patternRecordMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(records);

        PatternGenerateResponse response = PatternGenerateResponse.builder()
                .patternId("PAT-123")
                .build();
        when(patternGenerationStrategy.generatePattern(any(PatternGenerateRequest.class)))
                .thenReturn(response);

        assertDoesNotThrow(() -> offPeakBatchGenerator.executeBatchGeneration());
    }

    @Test
    @DisplayName("执行批量生成 - 异常不影响整体流程")
    void testExecuteBatchGeneration_ExceptionHandled() {
        when(patternRecordMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenThrow(new RuntimeException("数据库错误"));

        assertDoesNotThrow(() -> offPeakBatchGenerator.executeBatchGeneration());
    }

    private PatternRecord createPatternRecord(Long id, String name, Integer type) {
        PatternRecord record = new PatternRecord();
        record.setId(id);
        record.setPatternName(name);
        record.setPatternType(type);
        record.setBodySizeId(1L);
        record.setParameters("{}");
        record.setStatus(1);
        return record;
    }
}
