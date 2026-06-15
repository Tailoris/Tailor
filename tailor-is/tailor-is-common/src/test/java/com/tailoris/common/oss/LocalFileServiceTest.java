package com.tailoris.common.oss;

import com.tailoris.common.config.OssProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * LocalFileService 单元测试
 */
@DisplayName("LocalFileService 单元测试")
class LocalFileServiceTest {

    @TempDir
    Path tempDir;

    private OssProperties properties;
    private LocalFileService localFileService;

    @BeforeEach
    void setUp() {
        properties = new OssProperties();
        properties.setLocalFallbackPath(tempDir.toString());
        properties.setBasePath("tailoris");
        
        localFileService = new LocalFileService(properties);
        localFileService.init();
    }

    @Test
    @DisplayName("初始化应创建存储目录")
    void init_shouldCreateDirectory() {
        assertThat(tempDir).exists().isDirectory();
    }

    @Test
    @DisplayName("上传空文件应抛出异常")
    void upload_nullFile_shouldThrowException() {
        assertThatThrownBy(() -> localFileService.upload((MockMultipartFile) null, "test"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("文件不能为空");
    }

    @Test
    @DisplayName("上传空内容文件应抛出异常")
    void upload_emptyFile_shouldThrowException() {
        MockMultipartFile emptyFile = new MockMultipartFile("file", "", "image/jpeg", new byte[0]);
        
        assertThatThrownBy(() -> localFileService.upload(emptyFile, "test"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("文件不能为空");
    }

    @Test
    @DisplayName("上传MultipartFile应成功")
    void upload_multipartFile_shouldSucceed() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.jpg", "image/jpeg", "test content".getBytes());

        UploadResult result = localFileService.upload(file, "test");

        assertThat(result).isNotNull();
        assertThat(result.getStorageType()).isEqualTo("LOCAL");
        assertThat(result.getUrl()).startsWith("/upload/");
        assertThat(result.getSize()).isEqualTo(12L);
    }

    @Test
    @DisplayName("上传InputStream应成功并创建文件")
    void upload_inputStream_shouldSucceed() throws IOException {
        InputStream is = new ByteArrayInputStream("test content".getBytes());

        UploadResult result = localFileService.upload(is, "test.txt", 12L, "text/plain", "test");

        assertThat(result).isNotNull();
        assertThat(result.getStorageType()).isEqualTo("LOCAL");
        assertThat(result.getUrl()).startsWith("/upload/");
    }

    @Test
    @DisplayName("删除存在的文件应返回true")
    void delete_existingFile_shouldReturnTrue() throws IOException {
        Path testFile = tempDir.resolve("test-file.txt");
        Files.writeString(testFile, "test");

        boolean result = localFileService.delete("test-file.txt");

        assertThat(result).isTrue();
        assertThat(testFile).doesNotExist();
    }

    @Test
    @DisplayName("删除不存在的文件应返回false")
    void delete_nonExistingFile_shouldReturnFalse() {
        boolean result = localFileService.delete("non-existent.txt");

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("删除空key应返回false")
    void delete_emptyKey_shouldReturnFalse() {
        assertThat(localFileService.delete("")).isFalse();
        assertThat(localFileService.delete(null)).isFalse();
    }

    @Test
    @DisplayName("批量删除-空列表应返回0")
    void deleteBatch_emptyList_shouldReturnZero() {
        assertThat(localFileService.deleteBatch(Collections.emptyList())).isEqualTo(0);
        assertThat(localFileService.deleteBatch(null)).isEqualTo(0);
    }

    @Test
    @DisplayName("批量删除-应返回成功数")
    void deleteBatch_multipleKeys_shouldReturnSuccessCount() throws IOException {
        Path file1 = tempDir.resolve("file1.txt");
        Path file2 = tempDir.resolve("file2.txt");
        Files.writeString(file1, "test1");
        Files.writeString(file2, "test2");

        List<String> keys = Arrays.asList("file1.txt", "file2.txt", "file3.txt");
        int result = localFileService.deleteBatch(keys);

        assertThat(result).isEqualTo(2);
    }

    @Test
    @DisplayName("生成预签名URL应返回普通URL")
    void generatePresignedUrl_shouldReturnNormalUrl() {
        String url = localFileService.generatePresignedUrl("test/file.jpg", 3600);

        assertThat(url).isEqualTo("/upload/test/file.jpg");
    }

    @Test
    @DisplayName("获取访问URL应返回正确格式")
    void getAccessUrl_shouldReturnCorrectFormat() {
        String url = localFileService.getAccessUrl("tailoris/test/file.jpg");

        assertThat(url).isEqualTo("/upload/tailoris/test/file.jpg");
    }
}
