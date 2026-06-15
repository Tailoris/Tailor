package com.tailoris.ai.service.impl;

import com.tailoris.ai.dto.PatternGenerateRequest;
import com.tailoris.ai.dto.PatternGenerateResponse;
import com.tailoris.common.util.SpringSnowflakeIdGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * AI纸样生成服务测试 - 覆盖 B-M21
 *
 * @author Tailor IS Team
 */
@DisplayName("PatternGenerateServiceImpl 测试")
@ExtendWith(MockitoExtension.class)
class PatternGenerateServiceImplTest {

    @Mock
    private SpringSnowflakeIdGenerator snowflakeIdGenerator;

    @InjectMocks
    private PatternGenerateServiceImpl service;

    @BeforeEach
    void setUp() {
        lenient().when(snowflakeIdGenerator.nextId()).thenReturn(123456789012345L);
    }

    @Test
    @DisplayName("生成包含SVG完整结构的纸样")
    void shouldGenerateCompleteSvg() throws Exception {
        PatternGenerateRequest request = new PatternGenerateRequest();
        request.setGarmentType("T-Shirt");
        request.setWidth(new java.math.BigDecimal("200"));
        request.setHeight(new java.math.BigDecimal("300"));

        PatternGenerateResponse response = service.generatePattern(request);

        assertNotNull(response);
        assertNotNull(response.getPatternId());
        assertNotNull(response.getSvgContent());
        assertTrue(response.getSvgContent().contains("<?xml"));
        assertTrue(response.getSvgContent().contains("<svg"));
        assertTrue(response.getSvgContent().contains("</svg>"));
        assertTrue(response.getSvgContent().contains("T-Shirt"));
        assertTrue(response.getSvgContent().contains("Tailor IS AI Generated"));
    }

    @Test
    @DisplayName("空尺寸使用默认值")
    void shouldUseDefaultDimensions() throws Exception {
        PatternGenerateRequest request = new PatternGenerateRequest();
        request.setGarmentType("Shirt");
        // width和height为null

        PatternGenerateResponse response = service.generatePattern(request);
        assertNotNull(response);
        assertTrue(response.getSvgContent().contains("200"));
        assertTrue(response.getSvgContent().contains("300"));
    }

    @Test
    @DisplayName("生成的PaperID以PAT-开头")
    void shouldGenerateCorrectPatternId() {
        PatternGenerateRequest request = new PatternGenerateRequest();
        request.setGarmentType("Pants");

        PatternGenerateResponse response = service.generatePattern(request);
        assertNotNull(response.getPatternId());
        assertTrue(response.getPatternId().startsWith("PAT-"));
    }

    @Test
    @DisplayName("建议尺寸计算正确")
    void shouldCalculateRecommendedSizes() throws Exception {
        com.tailoris.ai.dto.BodySizeAnalysisRequest request = new com.tailoris.ai.dto.BodySizeAnalysisRequest();
        request.setChestCircumference(new java.math.BigDecimal("90"));
        request.setWaistCircumference(new java.math.BigDecimal("78"));

        com.tailoris.ai.dto.BodySizeAnalysisResponse response = service.analyzeBodySize(request);
        assertNotNull(response);
        assertEquals("M", response.getRecommendedSizes().get("top"));
        assertEquals("M", response.getRecommendedSizes().get("bottom"));
    }

    @Test
    @DisplayName("建议尺寸覆盖全部档位")
    void shouldHandleAllSizeRanges() throws Exception {
        // S码
        com.tailoris.ai.dto.BodySizeAnalysisRequest reqS = new com.tailoris.ai.dto.BodySizeAnalysisRequest();
        reqS.setChestCircumference(new java.math.BigDecimal("85"));
        assertEquals("S", service.analyzeBodySize(reqS).getRecommendedSizes().get("top"));

        // L码
        com.tailoris.ai.dto.BodySizeAnalysisRequest reqL = new com.tailoris.ai.dto.BodySizeAnalysisRequest();
        reqL.setChestCircumference(new java.math.BigDecimal("100"));
        assertEquals("L", service.analyzeBodySize(reqL).getRecommendedSizes().get("top"));

        // XL码
        com.tailoris.ai.dto.BodySizeAnalysisRequest reqXL = new com.tailoris.ai.dto.BodySizeAnalysisRequest();
        reqXL.setChestCircumference(new java.math.BigDecimal("110"));
        assertEquals("XL", service.analyzeBodySize(reqXL).getRecommendedSizes().get("top"));
    }
}
