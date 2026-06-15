package com.tailoris.ai.cache;

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

@DisplayName("PatternCacheLoader 单元测试")
@ExtendWith(MockitoExtension.class)
class PatternCacheLoaderTest {

    @Mock
    private CacheRouter cacheRouter;
    @Mock
    private PatternRecordMapper patternRecordMapper;
    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private PatternCacheLoader patternCacheLoader;

    @BeforeEach
    void setUp() {
        lenient().when(cacheRouter.getCoreTemplate()).thenReturn(redisTemplate);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    @DisplayName("启动时加载图案 - 空数据库")
    void testLoadPatternsOnStartup_EmptyDatabase() {
        when(patternRecordMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.emptyList());

        assertDoesNotThrow(() -> patternCacheLoader.loadPatternsOnStartup());
    }

    @Test
    @DisplayName("启动时加载图案 - 有图案数据")
    void testLoadPatternsOnStartup_WithData() {
        PatternRecord record1 = createPatternRecord(1L, "版型1", 1);
        PatternRecord record2 = createPatternRecord(2L, "版型2", 2);
        when(patternRecordMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Arrays.asList(record1, record2));

        assertDoesNotThrow(() -> patternCacheLoader.loadPatternsOnStartup());
        verify(valueOperations, atLeastOnce()).set(anyString(), any(), any());
    }

    @Test
    @DisplayName("启动时加载图案 - 数据库异常")
    void testLoadPatternsOnStartup_DatabaseException() {
        when(patternRecordMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenThrow(new RuntimeException("数据库错误"));

        assertDoesNotThrow(() -> patternCacheLoader.loadPatternsOnStartup());
    }

    @Test
    @DisplayName("定时刷新图案缓存 - 空数据库")
    void testRefreshPatterns_EmptyDatabase() {
        when(patternRecordMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.emptyList());

        assertDoesNotThrow(() -> patternCacheLoader.refreshPatterns());
    }

    @Test
    @DisplayName("定时刷新图案缓存 - 有图案数据")
    void testRefreshPatterns_WithData() {
        PatternRecord record1 = createPatternRecord(1L, "版型1", 1);
        PatternRecord record2 = createPatternRecord(2L, "版型2", 2);
        when(patternRecordMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Arrays.asList(record1, record2));

        assertDoesNotThrow(() -> patternCacheLoader.refreshPatterns());
        verify(valueOperations, atLeastOnce()).set(anyString(), any(), any());
    }

    @Test
    @DisplayName("定时刷新图案缓存 - Redis异常")
    void testRefreshPatterns_RedisException() {
        when(patternRecordMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Arrays.asList(createPatternRecord(1L, "版型1", 1)));
        doThrow(new RuntimeException("Redis错误"))
                .when(valueOperations).set(anyString(), any(), any());

        assertDoesNotThrow(() -> patternCacheLoader.refreshPatterns());
    }

    @Test
    @DisplayName("定时刷新图案缓存 - 数据库异常")
    void testRefreshPatterns_DatabaseException() {
        when(patternRecordMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenThrow(new RuntimeException("数据库错误"));

        assertDoesNotThrow(() -> patternCacheLoader.refreshPatterns());
    }

    @Test
    @DisplayName("按类型加载图案 - patternType为null")
    void testLoadPatternsByType_NullType() {
        PatternRecord record = new PatternRecord();
        record.setId(1L);
        record.setPatternType(null);
        record.setStatus(1);

        when(patternRecordMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Arrays.asList(record));

        assertDoesNotThrow(() -> patternCacheLoader.refreshPatterns());
    }

    @Test
    @DisplayName("按类型加载图案 - 多种类型")
    void testLoadPatternsByType_MultipleTypes() {
        PatternRecord record1 = createPatternRecord(1L, "版型1", 1);
        PatternRecord record2 = createPatternRecord(2L, "版型2", 2);
        PatternRecord record3 = createPatternRecord(3L, "版型3", 1);

        when(patternRecordMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Arrays.asList(record1, record2, record3));

        assertDoesNotThrow(() -> patternCacheLoader.refreshPatterns());
    }

    private PatternRecord createPatternRecord(Long id, String name, Integer type) {
        PatternRecord record = new PatternRecord();
        record.setId(id);
        record.setPatternName(name);
        record.setPatternType(type);
        record.setStatus(1);
        record.setThumbnailUrl("thumb.jpg");
        record.setPatternFileUrl("pattern.svg");
        return record;
    }
}
