package com.tailoris.common.oss;

import com.tailoris.common.config.OssProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * AliyunOssService 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AliyunOssService 单元测试")
class AliyunOssServiceTest {

    @Mock
    private OssProperties properties;

    @Mock
    private LocalFileService localFileService;

    private AliyunOssService ossService;

    @BeforeEach
    void setUp() {
        lenient().when(properties.isEnabled()).thenReturn(false);
        lenient().when(properties.getBasePath()).thenReturn("tailoris");
        lenient().when(properties.getMaxFileSize()).thenReturn(10 * 1024 * 1024L);
        lenient().when(properties.getAllowedTypeList()).thenReturn(Collections.emptyList());
        lenient().when(properties.getLocalFallbackPath()).thenReturn("/tmp/tailoris-test");
        
        ossService = new AliyunOssService(properties, localFileService);
    }

    @Test
    @DisplayName("上传空文件应抛出异常")
    void upload_nullFile_shouldThrowException() {
        assertThatThrownBy(() -> ossService.upload((MultipartFile) null, "test"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("文件不能为空");
    }

    @Test
    @DisplayName("上传空内容文件应抛出异常")
    void upload_emptyFile_shouldThrowException() {
        MockMultipartFile emptyFile = new MockMultipartFile("file", "", "image/jpeg", new byte[0]);
        
        assertThatThrownBy(() -> ossService.upload(emptyFile, "test"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("文件不能为空");
    }

    @Test
    @DisplayName("OSS不可用时应降级到本地存储")
    void upload_ossNotAvailable_shouldFallbackToLocal() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.jpg", "image/jpeg", "test content".getBytes());
        
        UploadResult expectedResult = UploadResult.ofLocal("key", "/upload/key", 12L, "image/jpeg");
        when(localFileService.upload(any(InputStream.class), anyString(), eq(12L), anyString(), anyString()))
                .thenReturn(expectedResult);

        UploadResult result = ossService.upload(file, "test");

        assertThat(result).isNotNull();
        assertThat(result.getStorageType()).isEqualTo("LOCAL");
    }

    @Test
    @DisplayName("删除文件-OSS不可用时应降级到本地存储")
    void delete_ossNotAvailable_shouldFallbackToLocal() {
        when(localFileService.delete("test-key")).thenReturn(true);

        boolean result = ossService.delete("test-key");

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("批量删除-空列表应返回0")
    void deleteBatch_emptyList_shouldReturnZero() {
        int result = ossService.deleteBatch(Collections.emptyList());
        assertThat(result).isEqualTo(0);
    }

    @Test
    @DisplayName("批量删除-null列表应返回0")
    void deleteBatch_nullList_shouldReturnZero() {
        int result = ossService.deleteBatch(null);
        assertThat(result).isEqualTo(0);
    }

    @Test
    @DisplayName("批量删除-应逐个删除并返回成功数")
    void deleteBatch_multipleKeys_shouldReturnSuccessCount() {
        List<String> keys = Arrays.asList("key1", "key2", "key3");
        when(localFileService.delete("key1")).thenReturn(true);
        when(localFileService.delete("key2")).thenReturn(true);
        when(localFileService.delete("key3")).thenReturn(false);

        int result = ossService.deleteBatch(keys);

        assertThat(result).isEqualTo(2);
    }

    @Test
    @DisplayName("生成预签名URL-OSS不可用时应降级到本地")
    void generatePresignedUrl_ossNotAvailable_shouldFallbackToLocal() {
        when(localFileService.generatePresignedUrl("test-key", 3600))
                .thenReturn("/upload/test-key");

        String url = ossService.generatePresignedUrl("test-key", 3600);

        assertThat(url).isEqualTo("/upload/test-key");
    }

    @Test
    @DisplayName("获取访问URL-无CDN时应返回OSS域名格式")
    void getAccessUrl_noCdn_shouldReturnOssDomain() {
        when(properties.getBucketName()).thenReturn("test-bucket");
        when(properties.getEndpoint()).thenReturn("oss-cn-shanghai.aliyuncs.com");
        when(properties.getCdnDomain()).thenReturn(null);

        String url = ossService.getAccessUrl("test/path/file.jpg");

        assertThat(url).isEqualTo("https://test-bucket.oss-cn-shanghai.aliyuncs.com/test/path/file.jpg");
    }

    @Test
    @DisplayName("获取访问URL-有CDN时应返回CDN地址")
    void getAccessUrl_withCdn_shouldReturnCdnUrl() {
        when(properties.getCdnDomain()).thenReturn("https://cdn.tailoris.com");

        String url = ossService.getAccessUrl("test/path/file.jpg");

        assertThat(url).isEqualTo("https://cdn.tailoris.com/test/path/file.jpg");
    }

    @Test
    @DisplayName("获取访问URL-CDN末尾有斜杠应正确处理")
    void getAccessUrl_cdnWithTrailingSlash_shouldHandleCorrectly() {
        when(properties.getCdnDomain()).thenReturn("https://cdn.tailoris.com/");

        String url = ossService.getAccessUrl("test/path/file.jpg");

        assertThat(url).isEqualTo("https://cdn.tailoris.com/test/path/file.jpg");
    }

    @Test
    @DisplayName("上传InputStream-OSS不可用时应降级")
    void uploadInputStream_ossNotAvailable_shouldFallback() throws IOException {
        InputStream is = new ByteArrayInputStream("test".getBytes());
        UploadResult expectedResult = UploadResult.ofLocal("key", "/upload/key", 4L, "text/plain");
        
        when(localFileService.upload(any(InputStream.class), anyString(), eq(4L), anyString(), anyString()))
                .thenReturn(expectedResult);

        UploadResult result = ossService.upload(is, "test.txt", 4L, "text/plain", "test");

        assertThat(result).isNotNull();
        assertThat(result.getStorageType()).isEqualTo("LOCAL");
    }
}
