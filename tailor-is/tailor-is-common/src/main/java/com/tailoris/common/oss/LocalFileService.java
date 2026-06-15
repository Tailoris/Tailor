package com.tailoris.common.oss;

import com.tailoris.common.config.OssProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

/**
 * 本地文件存储服务 - PRD-003.
 *
 * <p>降级方案：OSS 不可用时（如 dev 环境或 OSS 异常）使用本地文件系统。
 * 同时也可作为开发环境的默认存储。</p>
 *
 * <h3>路径结构</h3>
 * <pre>
 * {localFallbackPath}/
 *   {basePath}/
 *     {bizType}/
 *       yyyy/MM/dd/
 *         {uuid}.{ext}
 * </pre>
 *
 * @author Tailor IS Team
 * @since 1.0.0
 */
@Slf4j
@Service
public class LocalFileService implements ObjectStorageService {

    private final OssProperties properties;

    public LocalFileService(OssProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    public void init() {
        try {
            Path path = Paths.get(properties.getLocalFallbackPath());
            if (!Files.exists(path)) {
                Files.createDirectories(path);
                log.info("✅ 本地存储目录已创建: {}", path.toAbsolutePath());
            }
        } catch (IOException e) {
            log.error("本地存储目录创建失败: path={}", properties.getLocalFallbackPath(), e);
        }
    }

    @Override
    public UploadResult upload(MultipartFile file, String bizType) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }
        String key = generateKey(bizType, file.getOriginalFilename());
        try (InputStream is = file.getInputStream()) {
            return upload(is, file.getOriginalFilename(), file.getSize(), file.getContentType(), bizType);
        }
    }

    @Override
    public UploadResult upload(InputStream inputStream, String originalFilename, long size,
                                String contentType, String bizType) throws IOException {
        String key = generateKey(bizType, originalFilename);
        Path target = Paths.get(properties.getLocalFallbackPath()).resolve(key);
        Files.createDirectories(target.getParent());
        Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);

        // 本地访问URL（通过Nginx静态资源服务映射）
        String url = "/upload/" + key;
        log.info("本地存储上传成功: key={}, size={}", key, size);
        return UploadResult.ofLocal(key, url, size, contentType);
    }

    @Override
    public boolean delete(String objectKey) {
        if (!StringUtils.hasText(objectKey)) {
            return false;
        }
        try {
            Path file = Paths.get(properties.getLocalFallbackPath()).resolve(objectKey);
            return Files.deleteIfExists(file);
        } catch (IOException e) {
            log.error("本地文件删除失败: key={}", objectKey, e);
            return false;
        }
    }

    @Override
    public int deleteBatch(List<String> objectKeys) {
        if (objectKeys == null || objectKeys.isEmpty()) {
            return 0;
        }
        int success = 0;
        for (String key : objectKeys) {
            if (delete(key)) success++;
        }
        return success;
    }

    @Override
    public String generatePresignedUrl(String objectKey, long expireSeconds) {
        // 本地存储不支持预签名，返回普通URL（依赖Nginx访问控制）
        return getAccessUrl(objectKey);
    }

    @Override
    public String getAccessUrl(String objectKey) {
        return "/upload/" + objectKey;
    }

    private String generateKey(String bizType, String originalFilename) {
        String ext = "";
        if (originalFilename != null) {
            int dotIdx = originalFilename.lastIndexOf('.');
            if (dotIdx > 0) {
                ext = originalFilename.substring(dotIdx).toLowerCase();
            }
        }
        String datePath = java.time.LocalDate.now().format(
                java.time.format.DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String uuid = java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        return String.format("%s/%s/%s/%s%s",
                properties.getBasePath(),
                bizType == null ? "common" : bizType.toLowerCase(),
                datePath, uuid, ext);
    }
}
