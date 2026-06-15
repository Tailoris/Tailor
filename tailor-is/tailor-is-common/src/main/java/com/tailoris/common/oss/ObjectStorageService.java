package com.tailoris.common.oss;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * 对象存储服务接口 - PRD-003.
 *
 * <p>实现：</p>
 * <ul>
 *   <li>AliyunOssService: 阿里云 OSS（生产）</li>
 *   <li>LocalFileService: 本地文件系统（开发/降级）</li>
 * </ul>
 *
 * @author Tailor IS Team
 * @since 1.0.0
 */
public interface ObjectStorageService {

    /**
     * 上传文件.
     *
     * @param file   Spring MultipartFile
     * @param bizType 业务类型（product/review/avatar/...），用于组织存储路径
     * @return 上传结果
     */
    UploadResult upload(MultipartFile file, String bizType) throws IOException;

    /**
     * 上传字节流.
     */
    UploadResult upload(InputStream inputStream, String originalFilename, long size,
                        String contentType, String bizType) throws IOException;

    /**
     * 删除文件.
     */
    boolean delete(String objectKey);

    /**
     * 批量删除.
     */
    int deleteBatch(List<String> objectKeys);

    /**
     * 生成预签名URL（用于私有Bucket的临时访问）.
     */
    String generatePresignedUrl(String objectKey, long expireSeconds);

    /**
     * 获取访问URL.
     */
    String getAccessUrl(String objectKey);
}
