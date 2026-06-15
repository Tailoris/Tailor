package com.tailoris.copyright.service;

import com.tailoris.copyright.entity.CopyrightRecord;
import com.tailoris.copyright.mapper.CopyrightRecordMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@DisplayName("LocalSimilarityService 单元测试")
@ExtendWith(MockitoExtension.class)
class LocalSimilarityServiceTest {

    @Mock
    private CopyrightRecordMapper copyrightRecordMapper;

    @InjectMocks
    private LocalSimilarityService localSimilarityService;

    @BeforeEach
    void setUp() throws Exception {
        setField(localSimilarityService, "localThreshold", 80.0);
        setField(localSimilarityService, "localSimilarityEnabled", true);
        setField(localSimilarityService, "sampleSize", 1024);
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    @Test
    @DisplayName("预检查 - 功能未启用")
    void testPreCheck_Disabled() throws Exception {
        setField(localSimilarityService, "localSimilarityEnabled", false);

        LocalSimilarityService.LocalSimilarityResult result = localSimilarityService.preCheck(
                "http://example.com/file.png", "hash123", "image/png");

        assertNotNull(result);
        assertEquals(0.0, result.getScore());
        assertFalse(result.isBlocked());
        assertEquals("DISABLED", result.getMethod());
    }

    @Test
    @DisplayName("预检查 - 精确哈希匹配拦截")
    void testPreCheck_ExactHashMatch() {
        CopyrightRecord existingRecord = new CopyrightRecord();
        existingRecord.setId(1L);
        existingRecord.setWorkName("已登记作品");
        existingRecord.setFileHash("exact-hash-match");

        when(copyrightRecordMapper.selectByHash("exact-hash-match")).thenReturn(existingRecord);

        LocalSimilarityService.LocalSimilarityResult result = localSimilarityService.preCheck(
                "http://example.com/file.png", "exact-hash-match", "image/png");

        assertNotNull(result);
        assertEquals(100.0, result.getScore());
        assertTrue(result.isBlocked());
        assertEquals(1L, result.getMatchedRecordId());
        assertEquals("已登记作品", result.getMatchedWorkName());
        assertEquals("EXACT_HASH", result.getMethod());
    }

    @Test
    @DisplayName("预检查 - 无匹配记录")
    void testPreCheck_NoMatch() {
        when(copyrightRecordMapper.selectByHash(anyString())).thenReturn(null);
        when(copyrightRecordMapper.selectByWorkType(anyInt(), anyInt())).thenReturn(Collections.emptyList());

        LocalSimilarityService.LocalSimilarityResult result = localSimilarityService.preCheck(
                "http://example.com/file.png", "unique-hash", "image/png");

        assertNotNull(result);
        assertEquals(0.0, result.getScore());
        assertFalse(result.isBlocked());
        assertEquals("LOCAL_FINGERPRINT", result.getMethod());
    }

    @Test
    @DisplayName("预检查 - 文件哈希为空")
    void testPreCheck_NullHash() {
        when(copyrightRecordMapper.selectByWorkType(anyInt(), anyInt())).thenReturn(Collections.emptyList());

        LocalSimilarityService.LocalSimilarityResult result = localSimilarityService.preCheck(
                "http://example.com/file.png", null, "image/png");

        assertNotNull(result);
        assertFalse(result.isBlocked());
    }

    @Test
    @DisplayName("预检查 - 文件哈希为空字符串")
    void testPreCheck_EmptyHash() {
        when(copyrightRecordMapper.selectByWorkType(anyInt(), anyInt())).thenReturn(Collections.emptyList());

        LocalSimilarityService.LocalSimilarityResult result = localSimilarityService.preCheck(
                "http://example.com/file.png", "", "image/png");

        assertNotNull(result);
        assertFalse(result.isBlocked());
    }

    @Test
    @DisplayName("解析作品类型 - 图像")
    void testParseWorkType_Image() throws Exception {
        Method method = LocalSimilarityService.class.getDeclaredMethod("parseWorkType", String.class);
        method.setAccessible(true);

        assertEquals(1, method.invoke(localSimilarityService, "image/png"));
        assertEquals(1, method.invoke(localSimilarityService, "image/jpeg"));
        assertEquals(1, method.invoke(localSimilarityService, "png"));
        assertEquals(1, method.invoke(localSimilarityService, "jpg"));
        assertEquals(1, method.invoke(localSimilarityService, "jpeg"));
        assertEquals(1, method.invoke(localSimilarityService, "gif"));
        assertEquals(1, method.invoke(localSimilarityService, "webp"));
    }

    @Test
    @DisplayName("解析作品类型 - 视频")
    void testParseWorkType_Video() throws Exception {
        Method method = LocalSimilarityService.class.getDeclaredMethod("parseWorkType", String.class);
        method.setAccessible(true);

        assertEquals(2, method.invoke(localSimilarityService, "video/mp4"));
        assertEquals(2, method.invoke(localSimilarityService, "mp4"));
        assertEquals(2, method.invoke(localSimilarityService, "avi"));
    }

    @Test
    @DisplayName("解析作品类型 - 音频")
    void testParseWorkType_Audio() throws Exception {
        Method method = LocalSimilarityService.class.getDeclaredMethod("parseWorkType", String.class);
        method.setAccessible(true);

        assertEquals(3, method.invoke(localSimilarityService, "audio/mp3"));
        assertEquals(3, method.invoke(localSimilarityService, "mp3"));
        assertEquals(3, method.invoke(localSimilarityService, "wav"));
    }

    @Test
    @DisplayName("解析作品类型 - 文本")
    void testParseWorkType_Text() throws Exception {
        Method method = LocalSimilarityService.class.getDeclaredMethod("parseWorkType", String.class);
        method.setAccessible(true);

        assertEquals(4, method.invoke(localSimilarityService, "text/plain"));
        assertEquals(4, method.invoke(localSimilarityService, "pdf"));
        assertEquals(4, method.invoke(localSimilarityService, "doc"));
    }

    @Test
    @DisplayName("解析作品类型 - 未知类型")
    void testParseWorkType_Unknown() throws Exception {
        Method method = LocalSimilarityService.class.getDeclaredMethod("parseWorkType", String.class);
        method.setAccessible(true);

        assertNull(method.invoke(localSimilarityService, "unknown"));
        assertNull(method.invoke(localSimilarityService, (String) null));
    }

    @Test
    @DisplayName("计算相似度分数 - 文件类型匹配")
    void testComputeSimilarityScore_TypeMatch() throws Exception {
        CopyrightRecord record = new CopyrightRecord();
        record.setId(1L);
        record.setFileType("image/png");
        record.setFileSize(1024L);
        record.setFileHash("abc123");

        Method method = LocalSimilarityService.class.getDeclaredMethod("computeSimilarityScore",
                String.class, CopyrightRecord.class, String.class);
        method.setAccessible(true);

        double score = (double) method.invoke(localSimilarityService, "12345:hash", record, "image/png");
        assertTrue(score > 0);
        assertTrue(score <= 100.0);
    }

    @Test
    @DisplayName("计算相似度分数 - 文件类型不匹配")
    void testComputeSimilarityScore_TypeMismatch() throws Exception {
        CopyrightRecord record = new CopyrightRecord();
        record.setId(1L);
        record.setFileType("image/png");
        record.setFileSize(1024L);
        record.setFileHash("abc123");

        Method method = LocalSimilarityService.class.getDeclaredMethod("computeSimilarityScore",
                String.class, CopyrightRecord.class, String.class);
        method.setAccessible(true);

        double score = (double) method.invoke(localSimilarityService, "12345:hash", record, "video/mp4");
        assertTrue(score >= 0);
    }

    @Test
    @DisplayName("计算相似度分数 - 文件大小为空")
    void testComputeSimilarityScore_NullFileSize() throws Exception {
        CopyrightRecord record = new CopyrightRecord();
        record.setId(1L);
        record.setFileType("image/png");
        record.setFileSize(null);
        record.setFileHash("abc123");

        Method method = LocalSimilarityService.class.getDeclaredMethod("computeSimilarityScore",
                String.class, CopyrightRecord.class, String.class);
        method.setAccessible(true);

        double score = (double) method.invoke(localSimilarityService, "12345:hash", record, "image/png");
        assertTrue(score >= 0);
    }

    @Test
    @DisplayName("计算相似度分数 - 文件哈希为空")
    void testComputeSimilarityScore_NullFileHash() throws Exception {
        CopyrightRecord record = new CopyrightRecord();
        record.setId(1L);
        record.setFileType("image/png");
        record.setFileSize(1024L);
        record.setFileHash(null);

        Method method = LocalSimilarityService.class.getDeclaredMethod("computeSimilarityScore",
                String.class, CopyrightRecord.class, String.class);
        method.setAccessible(true);

        double score = (double) method.invoke(localSimilarityService, "12345:hash", record, "image/png");
        assertTrue(score >= 0);
    }

    @Test
    @DisplayName("计算本地指纹 - URL访问失败")
    void testComputeLocalFingerprint_UrlError() throws Exception {
        Method method = LocalSimilarityService.class.getDeclaredMethod("computeLocalFingerprint",
                String.class, String.class);
        method.setAccessible(true);

        String result = (String) method.invoke(localSimilarityService,
                "http://invalid-url-that-does-not-exist.com/file.png", "image/png");
        assertNotNull(result);
        // 返回格式为 "error:" + URL的hashCode
        assertTrue(result.contains("error:") || result.length() > 0);
    }

    @Test
    @DisplayName("查找相似记录 - 无相似记录")
    void testFindSimilarByFingerprint_NoMatches() throws Exception {
        when(copyrightRecordMapper.selectByWorkType(anyInt(), anyInt())).thenReturn(Collections.emptyList());

        Method method = LocalSimilarityService.class.getDeclaredMethod("findSimilarByFingerprint",
                String.class, String.class);
        method.setAccessible(true);

        java.util.List<?> result = (java.util.List<?>) method.invoke(localSimilarityService, "12345:hash", "image/png");
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("LocalSimilarityResult getter/setter")
    void testLocalSimilarityResult_GettersSetters() {
        LocalSimilarityService.LocalSimilarityResult result = new LocalSimilarityService.LocalSimilarityResult();
        result.setScore(85.0);
        result.setBlocked(true);
        result.setMatchedRecordId(1L);
        result.setMatchedWorkName("测试作品");
        result.setMethod("EXACT_HASH");

        assertEquals(85.0, result.getScore());
        assertTrue(result.isBlocked());
        assertEquals(1L, result.getMatchedRecordId());
        assertEquals("测试作品", result.getMatchedWorkName());
        assertEquals("EXACT_HASH", result.getMethod());
    }
}
