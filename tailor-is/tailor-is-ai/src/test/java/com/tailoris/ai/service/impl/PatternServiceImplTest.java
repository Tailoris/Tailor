package com.tailoris.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tailoris.ai.dto.PatternCheckRequest;
import com.tailoris.ai.dto.PatternGenerateRequest;
import com.tailoris.ai.dto.PatternIterationRequest;
import com.tailoris.ai.entity.BodySizeData;
import com.tailoris.ai.entity.PatternIteration;
import com.tailoris.ai.entity.PatternRecord;
import com.tailoris.ai.entity.PatternVersion;
import com.tailoris.ai.mapper.BodySizeDataMapper;
import com.tailoris.ai.mapper.PatternIterationMapper;
import com.tailoris.ai.mapper.PatternRecordMapper;
import com.tailoris.ai.mapper.PatternVersionMapper;
import com.tailoris.common.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("PatternServiceImpl 单元测试")
@ExtendWith(MockitoExtension.class)
class PatternServiceImplTest {

    @Mock
    private PatternRecordMapper patternRecordMapper;
    @Mock
    private PatternVersionMapper patternVersionMapper;
    @Mock
    private PatternIterationMapper patternIterationMapper;
    @Mock
    private BodySizeDataMapper bodySizeDataMapper;

    @Spy
    @InjectMocks
    private PatternServiceImpl patternService;

    private BodySizeData createBodySizeData() {
        BodySizeData data = new BodySizeData();
        data.setId(1L);
        data.setHeight(new BigDecimal("170"));
        data.setChestCircumference(new BigDecimal("96"));
        data.setWaistCircumference(new BigDecimal("82"));
        data.setHipCircumference(new BigDecimal("95"));
        return data;
    }

    @Test
    @DisplayName("生成版型 - 成功")
    void testGeneratePattern_Success() {
        BodySizeData bodySize = createBodySizeData();
        when(bodySizeDataMapper.selectById(1L)).thenReturn(bodySize);
        doReturn(1).when(patternRecordMapper).insert(any(PatternRecord.class));
        // 绕过 saveVersion，因为它使用了 LambdaUpdateWrapper
        doReturn(new PatternVersion()).when(patternService).saveVersion(anyLong(), anyString(), anyString());

        try (var mock = mockStatic(com.tailoris.common.util.SnowflakeIdGenerator.class)) {
            var gen = mock(com.tailoris.common.util.SnowflakeIdGenerator.class);
            mock.when(com.tailoris.common.util.SnowflakeIdGenerator::getInstance).thenReturn(gen);
            when(gen.nextId()).thenReturn(12345L);

            PatternGenerateRequest request = new PatternGenerateRequest();
            request.setBodySizeId(1L);
            request.setPatternType(1);
            request.setPatternName("测试版型");
            request.setParameters("{}");
            request.setExportFormat("SVG");

            PatternRecord result = patternService.generatePattern(100L, request);

            assertNotNull(result);
            assertEquals(100L, result.getUserId());
            assertEquals("测试版型", result.getPatternName());
            assertEquals(1, result.getVersion());
        }
    }

    @Test
    @DisplayName("生成版型 - 体型数据不存在抛异常")
    void testGeneratePattern_BodySizeNotFound() {
        when(bodySizeDataMapper.selectById(99L)).thenReturn(null);

        PatternGenerateRequest request = new PatternGenerateRequest();
        request.setBodySizeId(99L);

        assertThrows(BusinessException.class, () -> patternService.generatePattern(100L, request));
    }

    @Test
    @DisplayName("检查版型 - 成功")
    void testCheckPattern_Success() {
        PatternRecord record = new PatternRecord();
        record.setId(1L);
        record.setUserId(100L);
        record.setPatternData("{\"type\":\"1\"}");
        when(patternRecordMapper.selectById(1L)).thenReturn(record);
        doReturn(1).when(patternRecordMapper).updateById(any(PatternRecord.class));

        PatternCheckRequest request = new PatternCheckRequest();
        request.setPatternId(1L);

        String result = patternService.checkPattern(100L, request);

        assertNotNull(result);
        assertTrue(result.contains("valid"));
    }

    @Test
    @DisplayName("检查版型 - 记录不存在抛异常")
    void testCheckPattern_RecordNotFound() {
        when(patternRecordMapper.selectById(99L)).thenReturn(null);

        PatternCheckRequest request = new PatternCheckRequest();
        request.setPatternId(99L);

        assertThrows(BusinessException.class, () -> patternService.checkPattern(100L, request));
    }

    @Test
    @DisplayName("检查版型 - 非本人记录抛异常")
    void testCheckPattern_NotOwner() {
        PatternRecord record = new PatternRecord();
        record.setId(1L);
        record.setUserId(999L);
        when(patternRecordMapper.selectById(1L)).thenReturn(record);

        PatternCheckRequest request = new PatternCheckRequest();
        request.setPatternId(1L);

        assertThrows(BusinessException.class, () -> patternService.checkPattern(100L, request));
    }

    @Test
    @DisplayName("迭代版型 - 成功")
    void testIteratePattern_Success() {
        PatternRecord record = new PatternRecord();
        record.setId(1L);
        record.setUserId(100L);
        record.setBodySizeId(1L);
        record.setPatternType(1);
        record.setParameters("{\"old\":\"params\"}");
        record.setVersion(1);
        when(patternRecordMapper.selectById(1L)).thenReturn(record);

        BodySizeData bodySize = createBodySizeData();
        when(bodySizeDataMapper.selectById(1L)).thenReturn(bodySize);

        doReturn(1).when(patternIterationMapper).insert(any(PatternIteration.class));
        doReturn(1).when(patternRecordMapper).updateById(any(PatternRecord.class));
        // 绕过 saveVersion
        doReturn(new PatternVersion()).when(patternService).saveVersion(anyLong(), anyString(), anyString());

        try (var mock = mockStatic(com.tailoris.common.util.SnowflakeIdGenerator.class)) {
            var gen = mock(com.tailoris.common.util.SnowflakeIdGenerator.class);
            mock.when(com.tailoris.common.util.SnowflakeIdGenerator::getInstance).thenReturn(gen);
            when(gen.nextId()).thenReturn(12345L);

            PatternIterationRequest request = new PatternIterationRequest();
            request.setPatternId(1L);
            request.setIterationType(1);
            request.setNewParameters("{\"new\":\"params\"}");
            request.setChangeReason("调整尺寸");

            PatternIteration result = patternService.iteratePattern(100L, request);

            assertNotNull(result);
            assertEquals("{\"new\":\"params\"}", result.getNewParameters());
        }
    }

    @Test
    @DisplayName("迭代版型 - 记录不存在抛异常")
    void testIteratePattern_RecordNotFound() {
        when(patternRecordMapper.selectById(99L)).thenReturn(null);

        PatternIterationRequest request = new PatternIterationRequest();
        request.setPatternId(99L);

        assertThrows(BusinessException.class, () -> patternService.iteratePattern(100L, request));
    }

    @Test
    @DisplayName("保存版本 - 记录不存在抛异常")
    void testSaveVersion_RecordNotFound() {
        when(patternRecordMapper.selectById(99L)).thenReturn(null);

        assertThrows(BusinessException.class, () -> patternService.saveVersion(99L, "V1", "desc"));
    }

    @Test
    @DisplayName("查询版本列表")
    void testListVersions() {
        PatternVersion v1 = new PatternVersion();
        v1.setVersionNo(1);
        PatternVersion v2 = new PatternVersion();
        v2.setVersionNo(2);
        when(patternVersionMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Arrays.asList(v2, v1));

        List<PatternVersion> result = patternService.listVersions(1L);

        assertEquals(2, result.size());
    }

    @Test
    @DisplayName("导出版型 - 成功")
    void testExportPattern_Success() {
        PatternRecord record = new PatternRecord();
        record.setId(1L);
        when(patternRecordMapper.selectById(1L)).thenReturn(record);

        String result = patternService.exportPattern(1L, "SVG");

        assertNotNull(result);
        assertTrue(result.contains("1.svg"));
    }

    @Test
    @DisplayName("导出版型 - 记录不存在抛异常")
    void testExportPattern_RecordNotFound() {
        when(patternRecordMapper.selectById(99L)).thenReturn(null);

        assertThrows(BusinessException.class, () -> patternService.exportPattern(99L, "SVG"));
    }

    @Test
    @DisplayName("导出版型 - 默认SVG格式")
    void testExportPattern_DefaultFormat() {
        PatternRecord record = new PatternRecord();
        record.setId(1L);
        when(patternRecordMapper.selectById(1L)).thenReturn(record);

        String result = patternService.exportPattern(1L, null);

        assertNotNull(result);
        assertTrue(result.contains(".svg"));
    }

    @Test
    @DisplayName("获取版型详情")
    void testGetPatternDetail() {
        PatternRecord record = new PatternRecord();
        record.setId(1L);
        record.setPatternName("测试版型");
        when(patternRecordMapper.selectById(1L)).thenReturn(record);

        PatternRecord result = patternService.getPatternDetail(1L);

        assertNotNull(result);
        assertEquals("测试版型", result.getPatternName());
    }

    @Test
    @DisplayName("查询用户版型列表")
    void testListUserPatterns() {
        PatternRecord r1 = new PatternRecord();
        r1.setId(1L);
        PatternRecord r2 = new PatternRecord();
        r2.setId(2L);
        when(patternRecordMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Arrays.asList(r1, r2));

        List<PatternRecord> result = patternService.listUserPatterns(100L);

        assertEquals(2, result.size());
    }
}
