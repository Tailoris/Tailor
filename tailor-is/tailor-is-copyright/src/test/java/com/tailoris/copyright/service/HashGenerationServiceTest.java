package com.tailoris.copyright.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.SetOperations;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("HashGenerationService 单元测试")
@ExtendWith(MockitoExtension.class)
class HashGenerationServiceTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private SetOperations<String, String> setOperations;

    private HashGenerationService hashGenerationService;

    @BeforeEach
    void setUp() {
        hashGenerationService = new HashGenerationService(stringRedisTemplate);
        lenient().when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(stringRedisTemplate.opsForSet()).thenReturn(setOperations);
    }

    @Test
    @DisplayName("生成文件SHA-256哈希 - 成功")
    void testGenerateFileHash_Success() {
        String content = "test content for hashing";
        InputStream inputStream = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));

        String hash = hashGenerationService.generateFileHash(inputStream);

        assertNotNull(hash);
        assertEquals(64, hash.length()); // SHA-256 produces 64 hex characters
        assertTrue(hash.matches("[a-f0-9]+"));
    }

    @Test
    @DisplayName("生成文件哈希 - 空流")
    void testGenerateFileHash_EmptyStream() {
        InputStream inputStream = new ByteArrayInputStream(new byte[0]);

        String hash = hashGenerationService.generateFileHash(inputStream);

        assertNotNull(hash);
        assertEquals(64, hash.length());
    }

    @Test
    @DisplayName("加入待上链队列 - 成功")
    void testAddToPendingQueue_Success() {
        String fileHash = "abc123def456";
        Long copyrightId = 100L;

        doNothing().when(valueOperations).set(anyString(), anyString(), anyLong(), any(TimeUnit.class));
        when(setOperations.add(anyString(), anyString())).thenReturn(1L);

        hashGenerationService.addToPendingQueue(fileHash, copyrightId);

        verify(valueOperations, times(1)).set(
                eq("copyright:pending:" + fileHash),
                eq(copyrightId.toString()),
                eq(7L),
                eq(TimeUnit.DAYS)
        );
        verify(setOperations, times(1)).add("copyright:pending:queue", fileHash);
    }

    @Test
    @DisplayName("获取待上链队列数量 - 成功")
    void testGetPendingCount_Success() {
        when(setOperations.size("copyright:pending:queue")).thenReturn(5L);

        long count = hashGenerationService.getPendingCount();

        assertEquals(5L, count);
    }

    @Test
    @DisplayName("获取待上链队列数量 - 队列为空返回0")
    void testGetPendingCount_Empty() {
        when(setOperations.size("copyright:pending:queue")).thenReturn(null);

        long count = hashGenerationService.getPendingCount();

        assertEquals(0L, count);
    }

    @Test
    @DisplayName("弹出待上链哈希 - 成功")
    void testPopPendingHash_Success() {
        String expectedHash = "hash123";
        when(setOperations.pop("copyright:pending:queue")).thenReturn(expectedHash);

        String hash = hashGenerationService.popPendingHash();

        assertEquals(expectedHash, hash);
    }

    @Test
    @DisplayName("弹出待上链哈希 - 队列为空返回null")
    void testPopPendingHash_Empty() {
        when(setOperations.pop("copyright:pending:queue")).thenReturn(null);

        String hash = hashGenerationService.popPendingHash();

        assertNull(hash);
    }

    @Test
    @DisplayName("批量弹出待上链哈希 - 成功")
    void testPopPendingHashes_Success() {
        when(setOperations.pop("copyright:pending:queue"))
                .thenReturn("hash1")
                .thenReturn("hash2")
                .thenReturn("hash3")
                .thenReturn(null);

        Set<String> hashes = hashGenerationService.popPendingHashes(5);

        assertNotNull(hashes);
        assertEquals(3, hashes.size());
        assertTrue(hashes.contains("hash1"));
        assertTrue(hashes.contains("hash2"));
        assertTrue(hashes.contains("hash3"));
    }

    @Test
    @DisplayName("批量弹出待上链哈希 - 数量限制")
    void testPopPendingHashes_Limited() {
        when(setOperations.pop("copyright:pending:queue"))
                .thenReturn("hash1")
                .thenReturn("hash2");

        Set<String> hashes = hashGenerationService.popPendingHashes(2);

        assertEquals(2, hashes.size());
    }

    @Test
    @DisplayName("根据哈希获取版权ID - 成功")
    void testGetCopyrightIdByHash_Success() {
        String fileHash = "testhash";
        Long expectedId = 123L;
        when(valueOperations.get("copyright:pending:" + fileHash)).thenReturn(expectedId.toString());

        Long copyrightId = hashGenerationService.getCopyrightIdByHash(fileHash);

        assertEquals(expectedId, copyrightId);
    }

    @Test
    @DisplayName("根据哈希获取版权ID - 不存在返回null")
    void testGetCopyrightIdByHash_NotFound() {
        when(valueOperations.get("copyright:pending:nonexistent")).thenReturn(null);

        Long copyrightId = hashGenerationService.getCopyrightIdByHash("nonexistent");

        assertNull(copyrightId);
    }

    @Test
    @DisplayName("根据哈希获取版权ID - 格式错误返回null")
    void testGetCopyrightIdByHash_InvalidFormat() {
        when(valueOperations.get("copyright:pending:badhash")).thenReturn("not-a-number");

        Long copyrightId = hashGenerationService.getCopyrightIdByHash("badhash");

        assertNull(copyrightId);
    }

    @Test
    @DisplayName("从待上链队列移除 - 成功")
    void testRemoveFromPendingQueue_Success() {
        String fileHash = "removeme";
        when(stringRedisTemplate.delete(anyString())).thenReturn(true);
        when(setOperations.remove(anyString(), any())).thenReturn(1L);

        hashGenerationService.removeFromPendingQueue(fileHash);

        verify(setOperations, times(1)).remove("copyright:pending:queue", fileHash);
        verify(stringRedisTemplate, times(1)).delete("copyright:pending:" + fileHash);
    }

    @Test
    @DisplayName("缓存文件哈希元数据 - 成功")
    void testCacheHashMetadata_Success() {
        String fileHash = "hash123";
        String metadata = "{\"size\":1024,\"type\":\"png\"}";
        doNothing().when(valueOperations).set(anyString(), anyString(), anyLong(), any(TimeUnit.class));

        hashGenerationService.cacheHashMetadata(fileHash, metadata);

        verify(valueOperations, times(1)).set(
                eq("copyright:hash:" + fileHash),
                eq(metadata),
                eq(30L),
                eq(TimeUnit.DAYS)
        );
    }

    @Test
    @DisplayName("获取缓存的文件哈希元数据 - 成功")
    void testGetHashMetadata_Success() {
        String fileHash = "hash123";
        String expectedMetadata = "{\"size\":1024}";
        when(valueOperations.get("copyright:hash:" + fileHash)).thenReturn(expectedMetadata);

        String metadata = hashGenerationService.getHashMetadata(fileHash);

        assertEquals(expectedMetadata, metadata);
    }

    @Test
    @DisplayName("获取缓存的文件哈希元数据 - 不存在返回null")
    void testGetHashMetadata_NotFound() {
        when(valueOperations.get("copyright:hash:nonexistent")).thenReturn(null);

        String metadata = hashGenerationService.getHashMetadata("nonexistent");

        assertNull(metadata);
    }

    @Test
    @DisplayName("检查文件哈希是否存在 - 存在")
    void testIsHashExists_True() {
        String fileHash = "existinghash";
        when(stringRedisTemplate.hasKey("copyright:pending:" + fileHash)).thenReturn(true);

        boolean exists = hashGenerationService.isHashExists(fileHash);

        assertTrue(exists);
    }

    @Test
    @DisplayName("检查文件哈希是否存在 - 不存在")
    void testIsHashExists_False() {
        String fileHash = "nonexistenthash";
        when(stringRedisTemplate.hasKey("copyright:pending:" + fileHash)).thenReturn(false);

        boolean exists = hashGenerationService.isHashExists(fileHash);

        assertFalse(exists);
    }
}
