package com.tailoris.ai.service;

import com.tailoris.ai.config.AiModelConfig;
import com.tailoris.ai.dto.PatternGenerateResponse;
import com.tailoris.ai.entity.PatternRecord;
import com.tailoris.ai.mapper.PatternRecordMapper;
import com.tailoris.ai.model.PatternRequest;
import com.tailoris.ai.service.impl.AiModelServiceImpl;
import com.tailoris.common.util.SpringSnowflakeIdGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * AiModelService 单元测试 - TEST-P2-01.
 *
 * <p>测试 AI 模型集成的核心逻辑，使用 Mockito 模拟外部依赖。</p>
 * <ul>
 *   <li>generatePattern - 本地模型不可用时的 fallback SVG 生成</li>
 *   <li>checkStructure - 结构检查（含错误处理）</li>
 *   <li>iteratePattern - 版型迭代</li>
 *   <li>exportPattern - 版型导出</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AiModelService 单元测试")
class AiModelServiceTest {

    @Mock private AiModelConfig aiModelConfig;
    @Mock private PatternRecordMapper patternRecordMapper;
    @Mock private SpringSnowflakeIdGenerator snowflakeIdGenerator;

    @Spy
    @InjectMocks
    private AiModelServiceImpl aiModelService;

    @BeforeEach
    void setUp() {
        when(snowflakeIdGenerator.nextId()).thenReturn(123456789L);
        lenient().when(aiModelConfig.getProvider()).thenReturn("local");
        lenient().when(aiModelConfig.getModelName()).thenReturn("gpt-4");
        lenient().when(aiModelConfig.getApiKey()).thenReturn("test-key");
        lenient().when(aiModelConfig.getEndpointUrl()).thenReturn("https://test.api.com/v1");
        lenient().when(aiModelConfig.getLocalModelUrl()).thenReturn("http://localhost:8000/api");
        lenient().when(aiModelConfig.getMaxRetries()).thenReturn(3);
        lenient().when(aiModelConfig.getRetryDelayMs()).thenReturn(100L);
        lenient().when(aiModelConfig.getRetryBackoffMultiplier()).thenReturn(2.0);
        lenient().when(aiModelConfig.getTimeoutMs()).thenReturn(5000L);
        lenient().when(aiModelConfig.getConnectTimeoutMs()).thenReturn(2000L);
        lenient().when(aiModelConfig.isFallbackToCloud()).thenReturn(false);
        lenient().when(aiModelConfig.isModelAvailable()).thenReturn(false);
    }

    // ============================================================
    // generatePattern 测试
    // ============================================================

    @Test
    @DisplayName("生成版型 - 本地模型不可用且不允许云回退时生成fallback SVG")
    void generatePattern_LocalUnavailable_FallbackSvg() {
        PatternRequest request = PatternRequest.builder()
                .garmentType("SHIRT")
                .stylePreference("casual")
                .measurements(Map.of("width", 200.0, "height", 300.0))
                .build();

        when(aiModelConfig.getProvider()).thenReturn("local");
        when(aiModelConfig.isModelAvailable()).thenReturn(false);
        when(aiModelConfig.isFallbackToCloud()).thenReturn(false);

        PatternGenerateResponse response = aiModelService.generatePattern(request);

        assertNotNull(response);
        assertNotNull(response.getPatternId());
        assertTrue(response.getPatternId().startsWith("AI-"));
        assertEquals("SHIRT", response.getGarmentType());
        assertNotNull(response.getSvgContent());
        assertTrue(response.getSvgContent().contains("<svg"));
        assertTrue(response.getSvgContent().contains("SHIRT"));
        assertNotNull(response.getPreviewUrl());
        assertTrue(response.getPreviewUrl().contains("/api/v1/ai/pattern/preview/"));
        assertEquals(1, response.getVersion());
        assertNotNull(response.getCreatedAt());
    }

    @Test
    @DisplayName("生成版型 - 无测量数据时正常生成")
    void generatePattern_NoMeasurements() {
        PatternRequest request = PatternRequest.builder()
                .garmentType("DRESS")
                .stylePreference("formal")
                .build();

        when(aiModelConfig.getProvider()).thenReturn("local");
        when(aiModelConfig.isModelAvailable()).thenReturn(false);
        when(aiModelConfig.isFallbackToCloud()).thenReturn(false);

        PatternGenerateResponse response = aiModelService.generatePattern(request);

        assertNotNull(response);
        assertEquals("DRESS", response.getGarmentType());
        assertTrue(response.getSvgContent().contains("<svg"));
    }

    @Test
    @DisplayName("生成版型 - 包含约束条件")
    void generatePattern_WithConstraints() {
        PatternRequest request = PatternRequest.builder()
                .garmentType("JACKET")
                .stylePreference("business")
                .constraints("{\"max_width\": 250}")
                .measurements(Map.of("width", 220.0, "height", 350.0))
                .build();

        when(aiModelConfig.getProvider()).thenReturn("local");
        when(aiModelConfig.isModelAvailable()).thenReturn(false);
        when(aiModelConfig.isFallbackToCloud()).thenReturn(false);

        PatternGenerateResponse response = aiModelService.generatePattern(request);

        assertNotNull(response);
        assertTrue(response.getSvgContent().contains("JACKET"));
    }

    @Test
    @DisplayName("生成版型 - 空类型默认显示")
    void generatePattern_NullGarmentType() {
        PatternRequest request = PatternRequest.builder()
                .garmentType(null)
                .measurements(Map.of("width", 200.0, "height", 300.0))
                .build();

        when(aiModelConfig.getProvider()).thenReturn("local");
        when(aiModelConfig.isModelAvailable()).thenReturn(false);
        when(aiModelConfig.isFallbackToCloud()).thenReturn(false);

        PatternGenerateResponse response = aiModelService.generatePattern(request);

        assertNotNull(response);
        assertTrue(response.getSvgContent().contains("<svg"));
    }

    // ============================================================
    // checkStructure 测试
    // ============================================================

    @Test
    @DisplayName("检查结构 - 本地模型检查失败返回错误JSON")
    void checkStructure_Error() {
        byte[] pattern = "test pattern data".getBytes();
        when(aiModelConfig.getProvider()).thenReturn("local");
        when(aiModelConfig.isModelAvailable()).thenReturn(false);

        String result = aiModelService.checkStructure(pattern);

        assertNotNull(result);
        assertTrue(result.contains("\\\"structure\\\"") || result.contains("\"structure\""));
        assertTrue(result.contains("error") || result.contains("unknown"));
    }

    @Test
    @DisplayName("检查结构 - null输入")
    void checkStructure_NullInput() {
        when(aiModelConfig.getProvider()).thenReturn("local");
        when(aiModelConfig.isModelAvailable()).thenReturn(false);

        String result = aiModelService.checkStructure(null);

        assertNotNull(result);
        assertTrue(result.contains("error") || result.contains("unknown"));
    }

    // ============================================================
    // iteratePattern 测试
    // ============================================================

    @Test
    @DisplayName("迭代版型 - 成功")
    void iteratePattern_Success() {
        Long patternId = 1L;
        PatternRecord record = new PatternRecord();
        record.setId(patternId);
        record.setPatternType(1);
        record.setPatternName("测试版型");
        record.setVersion(1);
        when(patternRecordMapper.selectById(patternId)).thenReturn(record);

        when(aiModelConfig.getProvider()).thenReturn("local");
        when(aiModelConfig.isModelAvailable()).thenReturn(false);
        when(aiModelConfig.isFallbackToCloud()).thenReturn(false);

        PatternGenerateResponse response = aiModelService.iteratePattern(patternId, "调整肩宽");

        assertNotNull(response);
        assertNotNull(response.getPatternId());
        assertTrue(response.getPatternId().startsWith("AI-"));
        assertEquals(2, response.getVersion());
        assertTrue(response.getName().contains("迭代"));
    }

    @Test
    @DisplayName("迭代版型 - 记录不存在抛异常")
    void iteratePattern_RecordNotFound() {
        when(patternRecordMapper.selectById(99L)).thenReturn(null);

        assertThrows(RuntimeException.class,
                () -> aiModelService.iteratePattern(99L, "feedback"));
    }

    @Test
    @DisplayName("迭代版型 - 无版本号时默认版本2")
    void iteratePattern_NullVersion() {
        Long patternId = 1L;
        PatternRecord record = new PatternRecord();
        record.setId(patternId);
        record.setPatternType(1);
        record.setPatternName("测试版型");
        record.setVersion(null);
        when(patternRecordMapper.selectById(patternId)).thenReturn(record);

        when(aiModelConfig.getProvider()).thenReturn("local");
        when(aiModelConfig.isModelAvailable()).thenReturn(false);
        when(aiModelConfig.isFallbackToCloud()).thenReturn(false);

        PatternGenerateResponse response = aiModelService.iteratePattern(patternId, "feedback");

        assertNotNull(response);
        assertEquals(2, response.getVersion());
    }

    // ============================================================
    // exportPattern 测试
    // ============================================================

    @Test
    @DisplayName("导出版型 - SVG格式")
    void exportPattern_Svg() {
        Long patternId = 1L;
        PatternRecord record = new PatternRecord();
        record.setId(patternId);
        when(patternRecordMapper.selectById(patternId)).thenReturn(record);

        String result = aiModelService.exportPattern(patternId, "SVG");

        assertNotNull(result);
        assertTrue(result.contains("1.svg"));
        assertTrue(result.startsWith("https://pattern-export.tailoris.com/"));
    }

    @Test
    @DisplayName("导出版型 - PDF格式")
    void exportPattern_Pdf() {
        Long patternId = 2L;
        PatternRecord record = new PatternRecord();
        record.setId(patternId);
        when(patternRecordMapper.selectById(patternId)).thenReturn(record);

        String result = aiModelService.exportPattern(patternId, "PDF");

        assertNotNull(result);
        assertTrue(result.contains("2.pdf"));
    }

    @Test
    @DisplayName("导出版型 - 空格式默认SVG")
    void exportPattern_NullFormat() {
        Long patternId = 1L;
        PatternRecord record = new PatternRecord();
        record.setId(patternId);
        when(patternRecordMapper.selectById(patternId)).thenReturn(record);

        String result = aiModelService.exportPattern(patternId, null);

        assertNotNull(result);
        assertTrue(result.contains(".svg"));
    }

    @Test
    @DisplayName("导出版型 - 记录不存在抛异常")
    void exportPattern_RecordNotFound() {
        when(patternRecordMapper.selectById(99L)).thenReturn(null);

        assertThrows(RuntimeException.class,
                () -> aiModelService.exportPattern(99L, "SVG"));
    }
}