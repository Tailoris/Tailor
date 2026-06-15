package com.tailoris.copyright.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeUnit;

/**
 * 文件哈希本地生成与缓存服务
 *
 * <p>在作品上传时本地生成 SHA-256 哈希，并缓存到 Redis。
 * 用于批量上链的 pending 队列管理。</p>
 *
 * <p>Redis key 设计：
 * <ul>
 *   <li>{@code copyright:pending:{hash}} — 待上链记录，value 为 copyrightRecordId</li>
 *   <li>{@code copyright:hash:{hash}} — 哈希缓存，value 为文件元信息 JSON</li>
 * </ul>
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HashGenerationService {

    private final StringRedisTemplate stringRedisTemplate;

    private static final String PENDING_KEY_PREFIX = "copyright:pending:";
    private static final String HASH_CACHE_KEY_PREFIX = "copyright:hash:";
    private static final String PENDING_SET_KEY = "copyright:pending:queue";
    private static final long HASH_CACHE_TTL_DAYS = 30;
    private static final long PENDING_TTL_DAYS = 7;

    /**
     * 生成文件的 SHA-256 哈希值
     *
     * @param inputStream 文件输入流
     * @return SHA-256 十六进制字符串
     */
    public String generateFileHash(InputStream inputStream) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
            byte[] hashBytes = digest.digest();
            return bytesToHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            log.error("SHA-256 算法不可用", e);
            throw new RuntimeException("SHA-256 算法不可用", e);
        } catch (IOException e) {
            log.error("读取文件流失败", e);
            throw new RuntimeException("读取文件流失败", e);
        }
    }

    /**
     * 将字节数组转换为十六进制字符串
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /**
     * 将哈希记录加入待上链队列（Redis Set）
     *
     * @param fileHash       文件哈希值
     * @param copyrightId    版权记录ID
     */
    public void addToPendingQueue(String fileHash, Long copyrightId) {
        String pendingKey = PENDING_KEY_PREFIX + fileHash;
        stringRedisTemplate.opsForValue().set(pendingKey, copyrightId.toString(), PENDING_TTL_DAYS, TimeUnit.DAYS);
        stringRedisTemplate.opsForSet().add(PENDING_SET_KEY, fileHash);
        log.info("加入待上链队列: hash={}, copyrightId={}", fileHash, copyrightId);
    }

    /**
     * 获取当前待上链队列中的记录数
     */
    public long getPendingCount() {
        Long size = stringRedisTemplate.opsForSet().size(PENDING_SET_KEY);
        return size != null ? size : 0;
    }

    /**
     * 从待上链队列中弹出一个哈希
     *
     * @return 文件哈希值，队列为空时返回 null
     */
    public String popPendingHash() {
        return stringRedisTemplate.opsForSet().pop(PENDING_SET_KEY);
    }

    /**
     * 批量弹出待上链队列中的哈希
     *
     * @param count 弹出数量
     * @return 文件哈希值列表
     */
    public java.util.Set<String> popPendingHashes(int count) {
        java.util.Set<String> hashes = new java.util.HashSet<>();
        for (int i = 0; i < count; i++) {
            String hash = popPendingHash();
            if (hash == null) {
                break;
            }
            hashes.add(hash);
        }
        return hashes;
    }

    /**
     * 获取待上链队列中指定 hash 对应的版权记录ID
     */
    public Long getCopyrightIdByHash(String fileHash) {
        String value = stringRedisTemplate.opsForValue().get(PENDING_KEY_PREFIX + fileHash);
        if (value == null) {
            return null;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            log.warn("无法解析 copyrightId: {}", value);
            return null;
        }
    }

    /**
     * 从待上链队列中移除指定哈希（上链成功后调用）
     */
    public void removeFromPendingQueue(String fileHash) {
        stringRedisTemplate.opsForSet().remove(PENDING_SET_KEY, fileHash);
        stringRedisTemplate.delete(PENDING_KEY_PREFIX + fileHash);
    }

    /**
     * 缓存文件哈希元信息
     */
    public void cacheHashMetadata(String fileHash, String metadata) {
        stringRedisTemplate.opsForValue().set(
                HASH_CACHE_KEY_PREFIX + fileHash, metadata, HASH_CACHE_TTL_DAYS, TimeUnit.DAYS);
    }

    /**
     * 获取缓存的文件哈希元信息
     */
    public String getHashMetadata(String fileHash) {
        return stringRedisTemplate.opsForValue().get(HASH_CACHE_KEY_PREFIX + fileHash);
    }

    /**
     * 检查文件哈希是否已经存在（重复检测）
     */
    public boolean isHashExists(String fileHash) {
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(PENDING_KEY_PREFIX + fileHash));
    }
}
