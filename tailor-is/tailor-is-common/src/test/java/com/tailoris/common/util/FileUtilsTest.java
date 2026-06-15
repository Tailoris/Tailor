package com.tailoris.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("FileUtils 测试")
class FileUtilsTest {

    @Nested
    @DisplayName("文件大小格式化测试")
    class FormatFileSizeTests {

        @Test
        @DisplayName("负数返回 0 B")
        void testNegativeBytes() {
            assertEquals("0 B", FileUtils.formatFileSize(-1));
        }

        @Test
        @DisplayName("0 字节")
        void testZeroBytes() {
            assertEquals("0 B", FileUtils.formatFileSize(0));
        }

        @Test
        @DisplayName("小于 1 KB")
        void testBytesLessThanKB() {
            assertEquals("500 B", FileUtils.formatFileSize(500));
        }

        @Test
        @DisplayName("KB 级别")
        void testKB() {
            assertEquals("1 KB", FileUtils.formatFileSize(1024));
        }

        @Test
        @DisplayName("KB 带小数")
        void testKBWithDecimal() {
            assertEquals("1.5 KB", FileUtils.formatFileSize(1536));
        }

        @Test
        @DisplayName("MB 级别")
        void testMB() {
            assertEquals("1 MB", FileUtils.formatFileSize(1024 * 1024));
        }

        @Test
        @DisplayName("GB 级别")
        void testGB() {
            assertEquals("1 GB", FileUtils.formatFileSize(1024L * 1024 * 1024));
        }

        @Test
        @DisplayName("TB 级别")
        void testTB() {
            assertEquals("1 TB", FileUtils.formatFileSize(1024L * 1024 * 1024 * 1024));
        }
    }

    @Nested
    @DisplayName("文件类型判断测试")
    class FileTypeTests {

        @Test
        @DisplayName("图片文件判断 - 有效")
        void testIsImageFileValid() {
            assertTrue(FileUtils.isImageFile("photo.jpg"));
            assertTrue(FileUtils.isImageFile("photo.jpeg"));
            assertTrue(FileUtils.isImageFile("photo.png"));
            assertTrue(FileUtils.isImageFile("photo.gif"));
            assertTrue(FileUtils.isImageFile("photo.bmp"));
            assertTrue(FileUtils.isImageFile("photo.webp"));
            assertTrue(FileUtils.isImageFile("photo.svg"));
        }

        @Test
        @DisplayName("图片文件判断 - 大写扩展名")
        void testIsImageFileUpperCase() {
            assertTrue(FileUtils.isImageFile("photo.JPG"));
            assertTrue(FileUtils.isImageFile("photo.PNG"));
        }

        @Test
        @DisplayName("图片文件判断 - 无效")
        void testIsImageFileInvalid() {
            assertFalse(FileUtils.isImageFile("document.pdf"));
            assertFalse(FileUtils.isImageFile("archive.zip"));
        }

        @Test
        @DisplayName("图片文件判断 - null 和空白")
        void testIsImageFileBlank() {
            assertFalse(FileUtils.isImageFile(null));
            assertFalse(FileUtils.isImageFile(""));
            assertFalse(FileUtils.isImageFile("   "));
        }

        @Test
        @DisplayName("文档文件判断 - 有效")
        void testIsDocumentFileValid() {
            assertTrue(FileUtils.isDocumentFile("file.doc"));
            assertTrue(FileUtils.isDocumentFile("file.docx"));
            assertTrue(FileUtils.isDocumentFile("file.xls"));
            assertTrue(FileUtils.isDocumentFile("file.xlsx"));
            assertTrue(FileUtils.isDocumentFile("file.ppt"));
            assertTrue(FileUtils.isDocumentFile("file.pptx"));
            assertTrue(FileUtils.isDocumentFile("file.pdf"));
            assertTrue(FileUtils.isDocumentFile("file.txt"));
        }

        @Test
        @DisplayName("文档文件判断 - 无效")
        void testIsDocumentFileInvalid() {
            assertFalse(FileUtils.isDocumentFile("photo.jpg"));
            assertFalse(FileUtils.isDocumentFile(null));
        }

        @Test
        @DisplayName("压缩文件判断 - 有效")
        void testIsArchiveFileValid() {
            assertTrue(FileUtils.isArchiveFile("file.zip"));
            assertTrue(FileUtils.isArchiveFile("file.rar"));
            assertTrue(FileUtils.isArchiveFile("file.7z"));
            assertTrue(FileUtils.isArchiveFile("file.tar"));
            assertTrue(FileUtils.isArchiveFile("file.gz"));
        }

        @Test
        @DisplayName("压缩文件判断 - 无效")
        void testIsArchiveFileInvalid() {
            assertFalse(FileUtils.isArchiveFile("photo.jpg"));
            assertFalse(FileUtils.isArchiveFile(null));
        }

        @Test
        @DisplayName("视频文件判断 - 有效")
        void testIsVideoFileValid() {
            assertTrue(FileUtils.isVideoFile("video.mp4"));
            assertTrue(FileUtils.isVideoFile("video.avi"));
            assertTrue(FileUtils.isVideoFile("video.mkv"));
            assertTrue(FileUtils.isVideoFile("video.mov"));
            assertTrue(FileUtils.isVideoFile("video.wmv"));
            assertTrue(FileUtils.isVideoFile("video.flv"));
            assertTrue(FileUtils.isVideoFile("video.webm"));
        }

        @Test
        @DisplayName("视频文件判断 - 无效")
        void testIsVideoFileInvalid() {
            assertFalse(FileUtils.isVideoFile("photo.jpg"));
            assertFalse(FileUtils.isVideoFile(null));
        }
    }

    @Nested
    @DisplayName("文件扩展名测试")
    class FileExtensionTests {

        @Test
        @DisplayName("获取文件扩展名")
        void testGetFileExtension() {
            assertEquals("jpg", FileUtils.getFileExtension("photo.jpg"));
            assertEquals("gz", FileUtils.getFileExtension("archive.tar.gz"));
        }

        @Test
        @DisplayName("无扩展名")
        void testGetFileExtensionNoExt() {
            assertEquals("", FileUtils.getFileExtension("filename"));
        }

        @Test
        @DisplayName("以点结尾")
        void testGetFileExtensionEndsWithDot() {
            assertEquals("", FileUtils.getFileExtension("filename."));
        }

        @Test
        @DisplayName("null 和空白")
        void testGetFileExtensionBlank() {
            assertEquals("", FileUtils.getFileExtension(null));
            assertEquals("", FileUtils.getFileExtension(""));
        }

        @Test
        @DisplayName("获取不带扩展名的文件名")
        void testGetFileNameWithoutExtension() {
            assertEquals("photo", FileUtils.getFileNameWithoutExtension("photo.jpg"));
            assertEquals("archive.tar", FileUtils.getFileNameWithoutExtension("archive.tar.gz"));
        }

        @Test
        @DisplayName("无扩展名时返回原文件名")
        void testGetFileNameWithoutExtensionNoExt() {
            assertEquals("filename", FileUtils.getFileNameWithoutExtension("filename"));
        }

        @Test
        @DisplayName("null 和空白返回空字符串")
        void testGetFileNameWithoutExtensionBlank() {
            assertEquals("", FileUtils.getFileNameWithoutExtension(null));
            assertEquals("", FileUtils.getFileNameWithoutExtension(""));
        }
    }

    @Nested
    @DisplayName("文件名生成测试")
    class GenerateFileNameTests {

        @Test
        @DisplayName("生成带扩展名的文件名")
        void testGenerateFileNameWithExtension() {
            String result = FileUtils.generateFileName("photo.jpg");
            assertNotNull(result);
            assertTrue(result.endsWith(".jpg"));
            assertTrue(result.length() > 4);
        }

        @Test
        @DisplayName("生成不带扩展名的文件名")
        void testGenerateFileNameWithoutExtension() {
            String result = FileUtils.generateFileName("filename");
            assertNotNull(result);
            assertFalse(result.contains("."));
        }
    }

    @Nested
    @DisplayName("MD5 计算测试")
    class MD5Tests {

        @Test
        @DisplayName("计算文件 MD5")
        void testCalculateMD5File(@TempDir Path tempDir) throws IOException {
            Path filePath = tempDir.resolve("test.txt");
            Files.writeString(filePath, "Hello, World!");

            String md5 = FileUtils.calculateMD5(filePath.toFile());
            assertNotNull(md5);
            assertEquals(32, md5.length());
        }

        @Test
        @DisplayName("null 文件返回 null")
        void testCalculateMD5NullFile() {
            assertNull(FileUtils.calculateMD5((File) null));
        }

        @Test
        @DisplayName("不存在的文件返回 null")
        void testCalculateMD5NonExistentFile() {
            assertNull(FileUtils.calculateMD5(new File("/nonexistent/file.txt")));
        }

        @Test
        @DisplayName("计算 MultipartFile MD5")
        void testCalculateMD5MultipartFile() {
            MockMultipartFile multipartFile = new MockMultipartFile(
                "file", "test.txt", "text/plain", "Hello, World!".getBytes()
            );

            String md5 = FileUtils.calculateMD5(multipartFile);
            assertNotNull(md5);
            assertEquals(32, md5.length());
        }

        @Test
        @DisplayName("null MultipartFile 返回 null")
        void testCalculateMD5NullMultipartFile() {
            assertNull(FileUtils.calculateMD5((org.springframework.web.multipart.MultipartFile) null));
        }

        @Test
        @DisplayName("空 MultipartFile 返回 null")
        void testCalculateMD5EmptyMultipartFile() {
            MockMultipartFile emptyFile = new MockMultipartFile(
                "file", "test.txt", "text/plain", new byte[0]
            );
            assertNull(FileUtils.calculateMD5(emptyFile));
        }
    }

    @Nested
    @DisplayName("文件删除测试")
    class DeleteFileTests {

        @Test
        @DisplayName("删除文件")
        void testDeleteFile(@TempDir Path tempDir) throws IOException {
            Path filePath = tempDir.resolve("test.txt");
            Files.writeString(filePath, "test");

            assertTrue(Files.exists(filePath));
            FileUtils.deleteFile(filePath);
            assertFalse(Files.exists(filePath));
        }

        @Test
        @DisplayName("删除 null 路径不抛出异常")
        void testDeleteFileNull() {
            assertDoesNotThrow(() -> FileUtils.deleteFile((Path) null));
        }

        @Test
        @DisplayName("删除不存在的文件不抛出异常")
        void testDeleteFileNonExistent(@TempDir Path tempDir) {
            Path filePath = tempDir.resolve("nonexistent.txt");
            assertDoesNotThrow(() -> FileUtils.deleteFile(filePath));
        }

        @Test
        @DisplayName("删除 File 对象")
        void testDeleteFileObject(@TempDir Path tempDir) throws IOException {
            Path filePath = tempDir.resolve("test.txt");
            Files.writeString(filePath, "test");

            assertTrue(Files.exists(filePath));
            FileUtils.deleteFile(filePath.toFile());
            assertFalse(Files.exists(filePath));
        }

        @Test
        @DisplayName("删除 null File 不抛出异常")
        void testDeleteFileObjectNull() {
            assertDoesNotThrow(() -> FileUtils.deleteFile((File) null));
        }

        @Test
        @DisplayName("递归删除目录")
        void testDeleteDirectory(@TempDir Path tempDir) throws IOException {
            Path dirPath = tempDir.resolve("subdir");
            Files.createDirectory(dirPath);
            Files.writeString(dirPath.resolve("file1.txt"), "test1");
            Files.writeString(dirPath.resolve("file2.txt"), "test2");

            assertTrue(Files.exists(dirPath));
            FileUtils.deleteFile(dirPath.toFile());
            assertFalse(Files.exists(dirPath));
        }
    }

    @Nested
    @DisplayName("文件大小测试")
    class FileSizeTests {

        @Test
        @DisplayName("获取文件大小")
        void testGetFileSize(@TempDir Path tempDir) throws IOException {
            Path filePath = tempDir.resolve("test.txt");
            Files.writeString(filePath, "Hello");

            long size = FileUtils.getFileSize(filePath.toFile());
            assertEquals(5, size);
        }

        @Test
        @DisplayName("null 文件返回 0")
        void testGetFileSizeNull() {
            assertEquals(0, FileUtils.getFileSize(null));
        }

        @Test
        @DisplayName("不存在的文件返回 0")
        void testGetFileSizeNonExistent() {
            assertEquals(0, FileUtils.getFileSize(new File("/nonexistent/file.txt")));
        }
    }

    @Nested
    @DisplayName("文件路径校验测试")
    class ValidFilePathTests {

        @Test
        @DisplayName("有效路径")
        void testValidFilePath() {
            assertTrue(FileUtils.isValidFilePath("/uploads/image/photo.jpg"));
        }

        @Test
        @DisplayName("包含 .. 的无效路径")
        void testInvalidFilePathWithDotDot() {
            assertFalse(FileUtils.isValidFilePath("/uploads/../etc/passwd"));
        }

        @Test
        @DisplayName("包含反斜杠的无效路径")
        void testInvalidFilePathWithBackslash() {
            assertFalse(FileUtils.isValidFilePath("/uploads\\image"));
        }

        @Test
        @DisplayName("不以 / 开头的无效路径")
        void testInvalidFilePathNoSlash() {
            assertFalse(FileUtils.isValidFilePath("uploads/image"));
        }

        @Test
        @DisplayName("null 和空白返回 false")
        void testValidFilePathBlank() {
            assertFalse(FileUtils.isValidFilePath(null));
            assertFalse(FileUtils.isValidFilePath(""));
            assertFalse(FileUtils.isValidFilePath("   "));
        }
    }

    @Nested
    @DisplayName("MultipartFile 转 File 测试")
    class MultipartFileToFileTests {

        @Test
        @DisplayName("正常转换")
        void testMultipartFileToFile() throws IOException {
            MockMultipartFile multipartFile = new MockMultipartFile(
                "file", "test.txt", "text/plain", "Hello".getBytes()
            );

            File result = FileUtils.multipartFileToFile(multipartFile);
            assertNotNull(result);
            assertTrue(result.exists());
            assertTrue(result.length() > 0);

            // cleanup
            result.delete();
        }

        @Test
        @DisplayName("null 返回 null")
        void testMultipartFileToFileNull() throws IOException {
            assertNull(FileUtils.multipartFileToFile(null));
        }

        @Test
        @DisplayName("空文件返回 null")
        void testMultipartFileToFileEmpty() throws IOException {
            MockMultipartFile emptyFile = new MockMultipartFile(
                "file", "test.txt", "text/plain", new byte[0]
            );
            assertNull(FileUtils.multipartFileToFile(emptyFile));
        }
    }
}
