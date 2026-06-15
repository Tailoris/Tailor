package com.tailoris.common.oss;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * 文件上传门面服务 - PRD-003.
 *
 * <p>统一对外接口，内部按配置自动选择 OSS 或本地存储。
 * 提供：单文件上传、批量上传、删除、预签名URL 等能力。</p>
 *
 * <h3>使用示例</h3>
 * <pre>
 * UploadResult r = fileUploadService.upload(file, "product");
 * String url = r.getUrl();        // 访问URL
 * String key = r.getObjectKey();  // 存储Key（删除时用）
 * </pre>
 *
 * @author Tailor IS Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileUploadService {

    private final AliyunOssService aliyunOssService;
    private final LocalFileService localFileService;

    /**
     * 上传文件.
     */
    public UploadResult upload(MultipartFile file, String bizType) throws IOException {
        return aliyunOssService.upload(file, bizType);
    }

    /**
     * 批量上传.
     */
    public List<UploadResult> uploadBatch(List<MultipartFile> files, String bizType) throws IOException {
        if (files == null || files.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        return files.stream()
                .map(f -> {
                    try {
                        return upload(f, bizType);
                    } catch (IOException e) {
                        log.error("批量上传失败: name={}", f.getOriginalFilename(), e);
                        throw new RuntimeException(e);
                    }
                })
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * 删除文件.
     */
    public boolean delete(String objectKey) {
        return aliyunOssService.delete(objectKey);
    }

    /**
     * 批量删除.
     */
    public int deleteBatch(List<String> objectKeys) {
        return aliyunOssService.deleteBatch(objectKeys);
    }

    /**
     * 获取访问URL.
     */
    public String getAccessUrl(String objectKey) {
        return aliyunOssService.getAccessUrl(objectKey);
    }

    /**
     * 预签名URL.
     */
    public String generatePresignedUrl(String objectKey, long expireSeconds) {
        return aliyunOssService.generatePresignedUrl(objectKey, expireSeconds);
    }
}
