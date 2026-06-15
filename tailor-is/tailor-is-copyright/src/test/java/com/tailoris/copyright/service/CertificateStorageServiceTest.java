package com.tailoris.copyright.service;

import com.tailoris.common.oss.ObjectStorageService;
import com.tailoris.common.oss.UploadResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.InputStream;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("CertificateStorageService 单元测试")
@ExtendWith(MockitoExtension.class)
class CertificateStorageServiceTest {

    @Mock
    private ObjectStorageService objectStorageService;

    @InjectMocks
    private CertificateStorageService certificateStorageService;

    @BeforeEach
    void setUp() throws Exception {
        // 设置配置值
        setField(certificateStorageService, "certificateBucket", "test-bucket");
        setField(certificateStorageService, "basePath", "test/certificates");
        setField(certificateStorageService, "presignExpireSeconds", 86400L);
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    @Test
    @DisplayName("上传证书PDF - 成功")
    void testUploadCertificatePdf_Success() throws Exception {
        String certNo = "CR-123456";
        byte[] pdfContent = "test pdf content".getBytes();
        
        UploadResult uploadResult = new UploadResult();
        uploadResult.setUrl("https://oss.example.com/cert.pdf");
        uploadResult.setObjectKey("test/certificates/2026/06/14/CR-123456.pdf");
        
        when(objectStorageService.upload(any(InputStream.class), anyString(), anyLong(), anyString(), anyString()))
                .thenReturn(uploadResult);

        String url = certificateStorageService.uploadCertificatePdf(certNo, pdfContent);

        assertNotNull(url);
        assertEquals("https://oss.example.com/cert.pdf", url);
        verify(objectStorageService, times(1)).upload(any(InputStream.class), eq("CR-123456.pdf"), 
                eq((long) pdfContent.length), eq("application/pdf"), eq("copyright-certificate"));
    }

    @Test
    @DisplayName("上传证书PDF - 失败返回null")
    void testUploadCertificatePdf_Failure() throws Exception {
        String certNo = "CR-123456";
        byte[] pdfContent = "test pdf content".getBytes();
        
        when(objectStorageService.upload(any(InputStream.class), anyString(), anyLong(), anyString(), anyString()))
                .thenThrow(new java.io.IOException("Upload failed"));

        String url = certificateStorageService.uploadCertificatePdf(certNo, pdfContent);

        assertNull(url);
    }

    @Test
    @DisplayName("上传二维码 - 成功")
    void testUploadQrCode_Success() throws Exception {
        String certNo = "CR-123456";
        byte[] qrContent = "test qr content".getBytes();
        
        UploadResult uploadResult = new UploadResult();
        uploadResult.setUrl("https://oss.example.com/qr.png");
        uploadResult.setObjectKey("test/certificates/qr/2026/06/14/CR-123456.png");
        
        when(objectStorageService.upload(any(InputStream.class), anyString(), anyLong(), anyString(), anyString()))
                .thenReturn(uploadResult);

        String url = certificateStorageService.uploadQrCode(certNo, qrContent);

        assertNotNull(url);
        assertEquals("https://oss.example.com/qr.png", url);
    }

    @Test
    @DisplayName("上传二维码 - 失败返回null")
    void testUploadQrCode_Failure() throws Exception {
        String certNo = "CR-123456";
        byte[] qrContent = "test qr content".getBytes();
        
        when(objectStorageService.upload(any(InputStream.class), anyString(), anyLong(), anyString(), anyString()))
                .thenThrow(new java.io.IOException("Upload failed"));

        String url = certificateStorageService.uploadQrCode(certNo, qrContent);

        assertNull(url);
    }

    @Test
    @DisplayName("生成下载URL - 成功")
    void testGenerateDownloadUrl_Success() {
        String objectKey = "test/certificates/2026/06/14/CR-123456.pdf";
        String expectedUrl = "https://oss.example.com/presigned-url";
        
        when(objectStorageService.generatePresignedUrl(eq(objectKey), anyLong()))
                .thenReturn(expectedUrl);

        String url = certificateStorageService.generateDownloadUrl(objectKey);

        assertNotNull(url);
        assertEquals(expectedUrl, url);
    }

    @Test
    @DisplayName("生成下载URL - objectKey为空返回null")
    void testGenerateDownloadUrl_EmptyKey() {
        String url = certificateStorageService.generateDownloadUrl("");

        assertNull(url);
    }

    @Test
    @DisplayName("生成下载URL - objectKey为null返回null")
    void testGenerateDownloadUrl_NullKey() {
        String url = certificateStorageService.generateDownloadUrl(null);

        assertNull(url);
    }

    @Test
    @DisplayName("生成下载URL - 预签名失败回退到公开URL")
    void testGenerateDownloadUrl_FallbackToPublicUrl() {
        String objectKey = "test/certificates/2026/06/14/CR-123456.pdf";
        String publicUrl = "https://oss.example.com/public-url";
        
        when(objectStorageService.generatePresignedUrl(eq(objectKey), anyLong()))
                .thenThrow(new RuntimeException("Presign failed"));
        when(objectStorageService.getAccessUrl(objectKey)).thenReturn(publicUrl);

        String url = certificateStorageService.generateDownloadUrl(objectKey);

        assertNotNull(url);
        assertEquals(publicUrl, url);
    }

    @Test
    @DisplayName("获取公开URL - 成功")
    void testGetPublicUrl_Success() {
        String objectKey = "test/certificates/2026/06/14/CR-123456.pdf";
        String expectedUrl = "https://oss.example.com/public-url";
        
        when(objectStorageService.getAccessUrl(objectKey)).thenReturn(expectedUrl);

        String url = certificateStorageService.getPublicUrl(objectKey);

        assertNotNull(url);
        assertEquals(expectedUrl, url);
    }

    @Test
    @DisplayName("获取公开URL - objectKey为空返回null")
    void testGetPublicUrl_EmptyKey() {
        String url = certificateStorageService.getPublicUrl("");

        assertNull(url);
    }

    @Test
    @DisplayName("删除证书 - 成功")
    void testDeleteCertificate_Success() {
        String objectKey = "test/certificates/2026/06/14/CR-123456.pdf";
        
        when(objectStorageService.delete(objectKey)).thenReturn(true);

        certificateStorageService.deleteCertificate(objectKey);

        verify(objectStorageService, times(1)).delete(objectKey);
    }

    @Test
    @DisplayName("删除证书 - objectKey为空不执行")
    void testDeleteCertificate_EmptyKey() {
        certificateStorageService.deleteCertificate("");

        verify(objectStorageService, never()).delete(anyString());
    }

    @Test
    @DisplayName("上传证据元数据 - 成功")
    void testUploadEvidenceMetadata_Success() throws Exception {
        String certNo = "CR-123456";
        String metadata = "{\"hash\":\"abc123\",\"size\":1024}";
        
        UploadResult uploadResult = new UploadResult();
        uploadResult.setUrl("https://oss.example.com/metadata.json");
        uploadResult.setObjectKey("test/certificates/metadata/2026/06/14/CR-123456.json");
        
        when(objectStorageService.upload(any(InputStream.class), anyString(), anyLong(), anyString(), anyString()))
                .thenReturn(uploadResult);

        String url = certificateStorageService.uploadEvidenceMetadata(certNo, metadata);

        assertNotNull(url);
        assertEquals("https://oss.example.com/metadata.json", url);
    }

    @Test
    @DisplayName("上传证据元数据 - 失败返回null")
    void testUploadEvidenceMetadata_Failure() throws Exception {
        String certNo = "CR-123456";
        String metadata = "{\"hash\":\"abc123\"}";
        
        when(objectStorageService.upload(any(InputStream.class), anyString(), anyLong(), anyString(), anyString()))
                .thenThrow(new java.io.IOException("Upload failed"));

        String url = certificateStorageService.uploadEvidenceMetadata(certNo, metadata);

        assertNull(url);
    }

    @Test
    @DisplayName("计算文件哈希 - 成功")
    void testComputeFileHash_Success() {
        byte[] content = "test content".getBytes();

        String hash = certificateStorageService.computeFileHash(content);

        assertNotNull(hash);
        assertEquals(64, hash.length()); // SHA-256 produces 64 hex characters
        assertTrue(hash.matches("[a-f0-9]+"));
    }

    @Test
    @DisplayName("计算文件哈希 - 空内容")
    void testComputeFileHash_EmptyContent() {
        byte[] content = new byte[0];

        String hash = certificateStorageService.computeFileHash(content);

        assertNotNull(hash);
        assertEquals(64, hash.length());
    }
}
