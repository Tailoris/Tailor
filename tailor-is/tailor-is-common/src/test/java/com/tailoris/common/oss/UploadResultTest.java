package com.tailoris.common.oss;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("文件上传结果测试")
class UploadResultTest {

    @Test
    @DisplayName("默认构造")
    void testDefaultConstructor() {
        UploadResult result = new UploadResult();
        assertNull(result.getOriginalName());
        assertNull(result.getObjectKey());
        assertNull(result.getUrl());
        assertNull(result.getSize());
        assertNull(result.getContentType());
        assertNull(result.getStorageType());
        assertNull(result.getEtag());
    }

    @Test
    @DisplayName("全参数构造")
    void testAllArgsConstructor() {
        UploadResult result = new UploadResult(
            "test.jpg", "uploads/test.jpg", "http://example.com/test.jpg",
            1024L, "image/jpeg", "OSS", "etag123"
        );
        assertEquals("test.jpg", result.getOriginalName());
        assertEquals("uploads/test.jpg", result.getObjectKey());
        assertEquals("http://example.com/test.jpg", result.getUrl());
        assertEquals(1024L, result.getSize());
        assertEquals("image/jpeg", result.getContentType());
        assertEquals("OSS", result.getStorageType());
        assertEquals("etag123", result.getEtag());
    }

    @Test
    @DisplayName("创建OSS上传结果")
    void testOfOss() {
        UploadResult result = UploadResult.ofOss(
            "uploads/test.jpg", "http://oss.example.com/test.jpg",
            2048L, "image/jpeg", "etag456"
        );
        assertNull(result.getOriginalName());
        assertEquals("uploads/test.jpg", result.getObjectKey());
        assertEquals("http://oss.example.com/test.jpg", result.getUrl());
        assertEquals(2048L, result.getSize());
        assertEquals("image/jpeg", result.getContentType());
        assertEquals("OSS", result.getStorageType());
        assertEquals("etag456", result.getEtag());
    }

    @Test
    @DisplayName("创建本地上传结果")
    void testOfLocal() {
        UploadResult result = UploadResult.ofLocal(
            "uploads/test.jpg", "/upload/test.jpg",
            1024L, "image/jpeg"
        );
        assertNull(result.getOriginalName());
        assertEquals("uploads/test.jpg", result.getObjectKey());
        assertEquals("/upload/test.jpg", result.getUrl());
        assertEquals(1024L, result.getSize());
        assertEquals("image/jpeg", result.getContentType());
        assertEquals("LOCAL", result.getStorageType());
        assertNull(result.getEtag());
    }

    @Test
    @DisplayName("getter/setter")
    void testGettersSetters() {
        UploadResult result = new UploadResult();
        result.setOriginalName("test.png");
        result.setObjectKey("uploads/test.png");
        result.setUrl("http://example.com/test.png");
        result.setSize(512L);
        result.setContentType("image/png");
        result.setStorageType("LOCAL");
        result.setEtag("etag789");

        assertEquals("test.png", result.getOriginalName());
        assertEquals("uploads/test.png", result.getObjectKey());
        assertEquals("http://example.com/test.png", result.getUrl());
        assertEquals(512L, result.getSize());
        assertEquals("image/png", result.getContentType());
        assertEquals("LOCAL", result.getStorageType());
        assertEquals("etag789", result.getEtag());
    }

    @Test
    @DisplayName("OSS结果和本地结果的区别")
    void testOssVsLocal() {
        UploadResult ossResult = UploadResult.ofOss("key", "url", 100, "type", "etag");
        UploadResult localResult = UploadResult.ofLocal("key", "url", 100, "type");

        assertEquals("OSS", ossResult.getStorageType());
        assertEquals("LOCAL", localResult.getStorageType());
        assertNotNull(ossResult.getEtag());
        assertNull(localResult.getEtag());
    }
}
