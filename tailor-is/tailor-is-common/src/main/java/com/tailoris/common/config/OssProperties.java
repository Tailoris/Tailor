package com.tailoris.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 阿里云 OSS 配置 - PRD-003.
 *
 * <p>application.yml 示例：</p>
 * <pre>
 * tailoris:
 *   oss:
 *     enabled: true              # false=本地存储, true=OSS
 *     endpoint: oss-cn-shanghai.aliyuncs.com
 *     access-key-id: ${OSS_AK}
 *     access-key-secret: ${OSS_SK}
 *     bucket-name: tailor-is
 *     base-path: tailoris
 *     cdn-domain: https://cdn.tailoris.com
 *     max-file-size: 10485760    # 10MB
 *     allowed-types: jpg,jpeg,png,gif,webp,mp4
 *     local-fallback-path: /data/tailoris/upload
 * </pre>
 *
 * @author Tailor IS Team
 * @since 1.0.0
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "tailoris.oss")
public class OssProperties {

    /** 是否启用OSS（false则降级到本地存储） */
    private boolean enabled = false;

    /** OSS Endpoint */
    private String endpoint;

    /** AccessKey ID */
    private String accessKeyId;

    /** AccessKey Secret */
    private String accessKeySecret;

    /** Bucket 名称 */
    private String bucketName;

    /** 文件存储基础路径 */
    private String basePath = "tailoris";

    /** CDN 加速域名（可选，访问时优先用CDN） */
    private String cdnDomain;

    /** 单文件最大字节（默认10MB） */
    private long maxFileSize = 10 * 1024 * 1024L;

    /** 允许的文件扩展名（小写，逗号分隔） */
    private String allowedTypes = "jpg,jpeg,png,gif,webp,mp4";

    /** OSS不可用时的本地降级路径 */
    private String localFallbackPath = "/data/tailoris/upload";

    public java.util.List<String> getAllowedTypeList() {
        if (allowedTypes == null || allowedTypes.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        return java.util.Arrays.asList(allowedTypes.toLowerCase().split(","));
    }
}
