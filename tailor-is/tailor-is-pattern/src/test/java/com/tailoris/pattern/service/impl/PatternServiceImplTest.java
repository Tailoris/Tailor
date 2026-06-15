package com.tailoris.pattern.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tailoris.pattern.entity.Pattern;
import com.tailoris.pattern.mapper.PatternMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("版型服务实现测试")
class PatternServiceImplTest {

    @Mock
    private PatternMapper patternMapper;

    @InjectMocks
    private PatternServiceImpl patternService;

    private Pattern testPattern;

    @BeforeEach
    void setUp() {
        testPattern = new Pattern();
        testPattern.setId(1L);
        testPattern.setName("测试版型");
        testPattern.setDescription("测试描述");
        testPattern.setCategory("上装");
        testPattern.setImageUrl("http://example.com/image.jpg");
        testPattern.setDimensions("{\"width\": 100, \"height\": 200}");
        testPattern.setStatus(1);
        testPattern.setMerchantId(100L);
    }

    @Test
    @DisplayName("创建版型 - 成功")
    void testCreatePattern_Success() {
        // Given
        when(patternMapper.insert(any(Pattern.class))).thenReturn(1);

        // When
        Long result = patternService.createPattern(testPattern);

        // Then
        assertNotNull(result);
        verify(patternMapper, times(1)).insert(testPattern);
    }

    @Test
    @DisplayName("更新版型 - 成功")
    void testUpdatePattern_Success() {
        // Given
        Long patternId = 1L;
        when(patternMapper.updateById(any(Pattern.class))).thenReturn(1);

        // When
        patternService.updatePattern(patternId, testPattern);

        // Then
        assertEquals(patternId, testPattern.getId());
        verify(patternMapper, times(1)).updateById(testPattern);
    }

    @Test
    @DisplayName("删除版型 - 成功")
    void testDeletePattern_Success() {
        // Given
        Long patternId = 1L;
        when(patternMapper.deleteById(eq(patternId))).thenReturn(1);

        // When
        patternService.deletePattern(patternId);

        // Then
        verify(patternMapper, times(1)).deleteById(patternId);
    }

    @Test
    @DisplayName("根据ID获取版型 - 存在")
    void testGetPatternById_Exists() {
        // Given
        Long patternId = 1L;
        when(patternMapper.selectById(eq(patternId))).thenReturn(testPattern);

        // When
        Pattern result = patternService.getPatternById(patternId);

        // Then
        assertNotNull(result);
        assertEquals(patternId, result.getId());
        assertEquals("测试版型", result.getName());
        verify(patternMapper, times(1)).selectById(patternId);
    }

    @Test
    @DisplayName("根据ID获取版型 - 不存在")
    void testGetPatternById_NotExists() {
        // Given
        Long patternId = 999L;
        when(patternMapper.selectById(eq(patternId))).thenReturn(null);

        // When
        Pattern result = patternService.getPatternById(patternId);

        // Then
        assertNull(result);
        verify(patternMapper, times(1)).selectById(patternId);
    }

    @Test
    @DisplayName("根据商户ID查询版型列表")
    void testListByMerchantId() {
        // Given
        Long merchantId = 100L;
        List<Pattern> patterns = Arrays.asList(testPattern);
        when(patternMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(patterns);

        // When
        List<Pattern> result = patternService.listByMerchantId(merchantId);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(merchantId, result.get(0).getMerchantId());
        verify(patternMapper, times(1)).selectList(any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("根据商户ID查询版型列表 - 空结果")
    void testListByMerchantId_Empty() {
        // Given
        Long merchantId = 999L;
        when(patternMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Arrays.asList());

        // When
        List<Pattern> result = patternService.listByMerchantId(merchantId);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(patternMapper, times(1)).selectList(any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("分页查询版型 - 第一页")
    void testPagePatterns_FirstPage() {
        // Given
        int pageNum = 1;
        int pageSize = 10;
        Page<Pattern> mockPage = new Page<>(pageNum, pageSize);
        mockPage.setRecords(Arrays.asList(testPattern));
        mockPage.setTotal(1);

        when(patternMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                .thenReturn(mockPage);

        // When
        Page<Pattern> result = patternService.pagePatterns(pageNum, pageSize);

        // Then
        assertNotNull(result);
        assertEquals(pageNum, result.getCurrent());
        assertEquals(pageSize, result.getSize());
        assertEquals(1, result.getTotal());
        assertEquals(1, result.getRecords().size());
        verify(patternMapper, times(1)).selectPage(any(Page.class), any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("分页查询版型 - 空结果")
    void testPagePatterns_Empty() {
        // Given
        int pageNum = 1;
        int pageSize = 10;
        Page<Pattern> mockPage = new Page<>(pageNum, pageSize);
        mockPage.setRecords(Arrays.asList());
        mockPage.setTotal(0);

        when(patternMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                .thenReturn(mockPage);

        // When
        Page<Pattern> result = patternService.pagePatterns(pageNum, pageSize);

        // Then
        assertNotNull(result);
        assertEquals(pageNum, result.getCurrent());
        assertEquals(pageSize, result.getSize());
        assertEquals(0, result.getTotal());
        assertTrue(result.getRecords().isEmpty());
        verify(patternMapper, times(1)).selectPage(any(Page.class), any(LambdaQueryWrapper.class));
    }
}
