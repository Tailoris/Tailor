package com.tailoris.common.oss;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文件上传结果 DTO - PRD-003.
 *
 * @author Tailor IS Team
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UploadResult {

    /** 原始文件名 */
    private String originalName;

    /** 存储后的文件名（含路径） */
    private String objectKey;

    /** 访问URL（CDN或OSS域名） */
    private String url;

    /** 文件大小（字节） */
    private Long size;

    /** 内容类型（MIME） */
    private String contentType;

    /** 存储类型: OSS / LOCAL */
    private String storageType;

    /** ETag（OSS上传标识，可用于校验） */
    private String etag;

    public static UploadResult ofOss(String key, String url, long size, String contentType, String etag) {
        return new UploadResult(null, key, url, size, contentType, "OSS", etag);
    }

    public static UploadResult ofLocal(String key, String url, long size, String contentType) {
        return new UploadResult(null, key, url, size, contentType, "LOCAL", null);
    }
}
