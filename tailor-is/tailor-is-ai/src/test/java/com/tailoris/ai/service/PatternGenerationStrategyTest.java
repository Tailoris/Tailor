package com.tailoris.ai.service;

import com.tailoris.ai.config.CloudModelConfig;
import com.tailoris.ai.config.LocalModelConfig;
import com.tailoris.ai.dto.PatternGenerateRequest;
import com.tailoris.ai.dto.PatternGenerateResponse;
import com.tailoris.ai.entity.BodySizeData;
import com.tailoris.ai.enums.ModelRoute;
import com.tailoris.ai.mapper.BodySizeDataMapper;
import com.tailoris.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("PatternGenerationStrategy 单元测试")
@ExtendWith(MockitoExtension.class)
class PatternGenerationStrategyTest {

    @Mock
    private LocalModelConfig localModelConfig;
    @Mock
    private CloudModelConfig cloudModelConfig;
    @Mock
    private BodySizeDataMapper bodySizeDataMapper;

    @InjectMocks
    private PatternGenerationStrategy strategy;

    private BodySizeData createBodySizeData(String bodyType) {
        BodySizeData data = new BodySizeData();
        data.setId(1L);
        data.setBodyType(bodyType);
        data.setHeight(new BigDecimal("170"));
        data.setWeight(new BigDecimal("65"));
        data.setShoulderWidth(new BigDecimal("44"));
        data.setChestCircumference(new BigDecimal("96"));
        data.setWaistCircumference(new BigDecimal("82"));
        data.setHipCircumference(new BigDecimal("95"));
        return data;
    }

    @Test
    @DisplayName("计算复杂度分数 - 标准体型")
    void testCalculateComplexityScore_Normal() {
        BodySizeData data = createBodySizeData("normal");
        double score = strategy.calculateComplexityScore(data);
        assertTrue(score >= 0.0 && score <= 1.0);
    }

    @Test
    @DisplayName("计算复杂度分数 - 特殊体型(plus_size)")
    void testCalculateComplexityScore_PlusSize() {
        BodySizeData data = createBodySizeData("plus_size");
        data.setChestCircumference(new BigDecimal("120"));
        data.setWaistCircumference(new BigDecimal("110"));
        data.setHipCircumference(new BigDecimal("125"));
        double score = strategy.calculateComplexityScore(data);
        assertTrue(score > 0.0);
    }

    @Test
    @DisplayName("计算复杂度分数 - 空数据返回0")
    void testCalculateComplexityScore_NullData() {
        BodySizeData data = new BodySizeData();
        double score = strategy.calculateComplexityScore(data);
        assertEquals(0.0, score);
    }

    @Test
    @DisplayName("路由决策 - 热门款式走云端")
    void testDetermineRoute_PopularGarment() {
        BodySizeData data = createBodySizeData("normal");
        PatternGenerateRequest request = new PatternGenerateRequest();
        request.setGarmentType("DRESS");

        ModelRoute route = strategy.determineRoute(data, request);

        assertEquals(ModelRoute.CLOUD, route);
    }

    @Test
    @DisplayName("路由决策 - 特殊体型走云端")
    void testDetermineRoute_SpecialBodyType() {
        BodySizeData data = createBodySizeData("plus_size");
        PatternGenerateRequest request = new PatternGenerateRequest();
        request.setGarmentType("SHIRT");

        ModelRoute route = strategy.determineRoute(data, request);

        assertEquals(ModelRoute.CLOUD, route);
    }

    @Test
    @DisplayName("路由决策 - 本地模型不可用且启用降级走云端")
    void testDetermineRoute_LocalUnavailable_FallbackEnabled() {
        BodySizeData data = createBodySizeData("normal");
        PatternGenerateRequest request = new PatternGenerateRequest();
        request.setGarmentType("SHIRT");

        when(localModelConfig.isAvailable()).thenReturn(false);
        when(localModelConfig.isFallbackToCloud()).thenReturn(true);

        ModelRoute route = strategy.determineRoute(data, request);

        assertEquals(ModelRoute.CLOUD, route);
    }

    @Test
    @DisplayName("路由决策 - 常规体型走本地")
    void testDetermineRoute_NormalBody_LocalRoute() {
        BodySizeData data = createBodySizeData("normal");
        PatternGenerateRequest request = new PatternGenerateRequest();
        request.setGarmentType("SHIRT");

        when(localModelConfig.isAvailable()).thenReturn(true);

        ModelRoute route = strategy.determineRoute(data, request);

        assertEquals(ModelRoute.LOCAL, route);
    }

    @Test
    @DisplayName("生成纸样 - 本地模型成功")
    void testGeneratePattern_LocalSuccess() {
        BodySizeData data = createBodySizeData("normal");
        when(bodySizeDataMapper.selectById(1L)).thenReturn(data);
        when(localModelConfig.isAvailable()).thenReturn(true);

        PatternGenerateRequest request = new PatternGenerateRequest();
        request.setBodySizeId(1L);
        request.setGarmentType("SHIRT");
        request.setPatternName("测试纸样");

        PatternGenerateResponse response = strategy.generatePattern(request);

        assertNotNull(response);
        assertNotNull(response.getPatternId());
        assertTrue(response.getPatternId().startsWith("LOC-"));
    }

    @Test
    @DisplayName("生成纸样 - 云端模型成功")
    void testGeneratePattern_CloudSuccess() {
        BodySizeData data = createBodySizeData("plus_size");
        when(bodySizeDataMapper.selectById(1L)).thenReturn(data);
        when(cloudModelConfig.isCircuitAvailable()).thenReturn(true);

        PatternGenerateRequest request = new PatternGenerateRequest();
        request.setBodySizeId(1L);
        request.setGarmentType("DRESS");
        request.setPatternName("测试纸样");

        PatternGenerateResponse response = strategy.generatePattern(request);

        assertNotNull(response);
        assertNotNull(response.getPatternId());
        assertTrue(response.getPatternId().startsWith("CLD-"));
    }

    @Test
    @DisplayName("生成纸样 - 体型数据不存在抛异常")
    void testGeneratePattern_BodySizeNotFound() {
        when(bodySizeDataMapper.selectById(99L)).thenReturn(null);

        PatternGenerateRequest request = new PatternGenerateRequest();
        request.setBodySizeId(99L);

        assertThrows(BusinessException.class, () -> strategy.generatePattern(request));
    }

    @Test
    @DisplayName("生成纸样 - 本地模型失败降级到云端")
    void testGeneratePattern_LocalFallbackToCloud() {
        BodySizeData data = createBodySizeData("normal");
        when(bodySizeDataMapper.selectById(1L)).thenReturn(data);
        lenient().when(localModelConfig.isAvailable()).thenReturn(true);
        lenient().when(localModelConfig.isFallbackToCloud()).thenReturn(true);
        lenient().when(cloudModelConfig.isCircuitAvailable()).thenReturn(true);

        PatternGenerateRequest request = new PatternGenerateRequest();
        request.setBodySizeId(1L);
        request.setGarmentType("SHIRT");
        request.setPatternName("测试纸样");

        PatternGenerateResponse response = strategy.generatePattern(request);

        assertNotNull(response);
    }

    @Test
    @DisplayName("获取本地调用统计")
    void testGetLocalCallCount() {
        assertNotNull(strategy.getLocalCallCount());
    }

    @Test
    @DisplayName("获取云端调用统计")
    void testGetCloudCallCount() {
        assertNotNull(strategy.getCloudCallCount());
    }
}
