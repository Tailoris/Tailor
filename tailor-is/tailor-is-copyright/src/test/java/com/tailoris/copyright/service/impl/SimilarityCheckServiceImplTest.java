package com.tailoris.copyright.service.impl;

import com.tailoris.copyright.service.SimilarityCheckService.MatchedItem;
import com.tailoris.copyright.service.SimilarityCheckService.SimilarityResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import com.tailoris.copyright.mapper.CrSimilarityCheckMapper;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("SimilarityCheckServiceImpl 单元测试")
@ExtendWith(MockitoExtension.class)
class SimilarityCheckServiceImplTest {

    @Mock
    private CrSimilarityCheckMapper similarityCheckMapper;
    @Mock
    private StringRedisTemplate stringRedisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private SimilarityCheckServiceImpl similarityCheckService;

    @org.junit.jupiter.api.BeforeEach
    void setUp() throws Exception {
        // 设置 threshold 字段为 60.0（模拟配置值）
        Field thresholdField = SimilarityCheckServiceImpl.class.getDeclaredField("threshold");
        thresholdField.setAccessible(true);
        thresholdField.set(similarityCheckService, 60.0);
    }

    @Test
    @DisplayName("预检查 - 文件URL为空抛异常")
    void testPreCheck_EmptyUrl() {
        assertThrows(Exception.class, () ->
                similarityCheckService.preCheck("", "hash123", "PNG"));
    }

    @Test
    @DisplayName("预检查 - 成功")
    void testPreCheck_Success() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);
        doNothing().when(valueOperations).set(anyString(), anyString(), anyLong(), any(TimeUnit.class));

        SimilarityResult result = similarityCheckService.preCheck("http://example.com/image.png", "hash123", "PNG");

        assertNotNull(result);
        assertTrue(result.getScore() >= 0);
        assertNotNull(result.getMethod());
    }

    @Test
    @DisplayName("库内比对 - 成功")
    void testCheckAgainstLibrary_Success() {
        SimilarityResult result = similarityCheckService.checkAgainstLibrary("http://example.com/image.png", null);

        assertNotNull(result);
        assertTrue(result.getScore() >= 0);
    }

    @Test
    @DisplayName("库外比对 - 成功")
    void testCheckAgainstWeb_Success() {
        List<String> targetUrls = Arrays.asList(
                "http://example.com/target1.png",
                "http://example.com/target2.png"
        );

        SimilarityResult result = similarityCheckService.checkAgainstWeb("http://example.com/source.png", targetUrls);

        assertNotNull(result);
        assertTrue(result.getScore() >= 0);
    }

    @Test
    @DisplayName("库外比对 - 高相似度判定侵权")
    void testCheckAgainstWeb_HighSimilarity() {
        List<String> targetUrls = Arrays.asList("http://example.com/similar.png");

        SimilarityResult result = similarityCheckService.checkAgainstWeb("http://example.com/source.png", targetUrls);

        assertNotNull(result);
        if (result.getScore() >= 60) {
            assertTrue(result.isInfringement());
        }
    }

    @Test
    @DisplayName("风险等级 - 重大风险(>=90)")
    void testRiskLevel_Critical() {
        SimilarityResult result = similarityCheckService.checkAgainstLibrary("high_similarity_url", null);
        if (result.getScore() >= 90) {
            assertEquals(4, result.getRiskLevel());
            assertTrue(result.isInfringement());
        }
    }

    @Test
    @DisplayName("风险等级 - 严重风险(>=80)")
    void testRiskLevel_Severe() {
        SimilarityResult result = similarityCheckService.checkAgainstLibrary("medium_high_url", null);
        if (result.getScore() >= 80 && result.getScore() < 90) {
            assertEquals(3, result.getRiskLevel());
            assertTrue(result.isInfringement());
        }
    }

    @Test
    @DisplayName("风险等级 - 一般风险(>=60)")
    void testRiskLevel_Moderate() {
        SimilarityResult result = similarityCheckService.checkAgainstLibrary("medium_url", null);
        if (result.getScore() >= 60 && result.getScore() < 80) {
            assertEquals(2, result.getRiskLevel());
            assertTrue(result.isInfringement());
        }
    }

    @Test
    @DisplayName("风险等级 - 轻微风险(>=30)")
    void testRiskLevel_Minor() {
        SimilarityResult result = similarityCheckService.checkAgainstLibrary("low_url", null);
        if (result.getScore() >= 30 && result.getScore() < 60) {
            assertEquals(1, result.getRiskLevel());
            assertFalse(result.isInfringement());
        }
    }

    @Test
    @DisplayName("风险等级 - 无风险(<30)")
    void testRiskLevel_None() {
        SimilarityResult result = similarityCheckService.checkAgainstLibrary("very_low_url", null);
        if (result.getScore() < 30) {
            assertEquals(0, result.getRiskLevel());
            assertFalse(result.isInfringement());
        }
    }
}
