package com.tailoris.copyright.service;

import com.tailoris.common.oss.ObjectStorageService;
import com.tailoris.common.oss.UploadResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 证据证书 OSS 存储服务
 *
 * <p>将证据证书存储到 OSS（而非链上），链上仅保存哈希值。
 * 支持生成证书下载 URL，用于证书检索和下载。</p>
 *
 * <p>存储策略：
 * <ul>
 *   <li>证书 PDF 上传到 OSS，路径: tailoris/copyright/certificates/{year}/{month}/{certNo}.pdf</li>
 *   <li>二维码图片上传到 OSS，路径: tailoris/copyright/qrcodes/{year}/{month}/{certNo}.png</li>
 *   <li>链上仅保存文件哈希值，不保存完整证书文件</li>
 *   <li>通过预签名 URL 或 CDN URL 提供下载</li>
 * </ul>
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CertificateStorageService {

    private final ObjectStorageService objectStorageService;

    @Value("${copyright.certificate-storage.bucket:copyright-certificates}")
    private String certificateBucket;

    @Value("${copyright.certificate-storage.base-path:tailoris/copyright/certificates}")
    private String basePath;

    @Value("${copyright.certificate-storage.presign-expire-seconds:86400}")
    private long presignExpireSeconds;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    /**
     * 上传证书 PDF 到 OSS
     *
     * @param certNo     证书编号
     * @param pdfContent PDF 文件字节数组
     * @return 证书访问 URL
     */
    public String uploadCertificatePdf(String certNo, byte[] pdfContent) {
        String objectKey = generateCertificateKey(certNo, "pdf");
        try (InputStream is = new ByteArrayInputStream(pdfContent)) {
            UploadResult result = objectStorageService.upload(
                    is, certNo + ".pdf", pdfContent.length, "application/pdf", "copyright-certificate");
            log.info("证书 PDF 上传成功: certNo={}, objectKey={}, url={}", certNo, result.getObjectKey(), result.getUrl());
            return result.getUrl();
        } catch (IOException e) {
            log.error("证书 PDF 上传失败: certNo={}", certNo, e);
            return null;
        }
    }

    /**
     * 上传证书二维码到 OSS
     *
     * @param certNo    证书编号
     * @param qrContent 二维码图片字节数组
     * @return 二维码访问 URL
     */
    public String uploadQrCode(String certNo, byte[] qrContent) {
        String objectKey = generateQrKey(certNo, "png");
        try (InputStream is = new ByteArrayInputStream(qrContent)) {
            UploadResult result = objectStorageService.upload(
                    is, certNo + ".png", qrContent.length, "image/png", "copyright-qrcode");
            log.info("二维码上传成功: certNo={}, objectKey={}, url={}", certNo, result.getObjectKey(), result.getUrl());
            return result.getUrl();
        } catch (IOException e) {
            log.error("二维码上传失败: certNo={}", certNo, e);
            return null;
        }
    }

    /**
     * 生成证书下载 URL（预签名 URL，适用于私有 Bucket）
     *
     * @param objectKey OSS 对象键
     * @return 预签名下载 URL
     */
    public String generateDownloadUrl(String objectKey) {
        if (objectKey == null || objectKey.isEmpty()) {
            log.warn("生成下载 URL 失败：objectKey 为空");
            return null;
        }
        try {
            return objectStorageService.generatePresignedUrl(objectKey, presignExpireSeconds);
        } catch (Exception e) {
            log.error("生成预签名 URL 失败: objectKey={}", objectKey, e);
            return objectStorageService.getAccessUrl(objectKey);
        }
    }

    /**
     * 获取证书公开访问 URL（适用于 CDN 或公开 Bucket）
     *
     * @param objectKey OSS 对象键
     * @return 公开访问 URL
     */
    public String getPublicUrl(String objectKey) {
        if (objectKey == null || objectKey.isEmpty()) {
            return null;
        }
        return objectStorageService.getAccessUrl(objectKey);
    }

    /**
     * 删除证书文件
     *
     * @param objectKey OSS 对象键
     */
    public void deleteCertificate(String objectKey) {
        if (objectKey == null || objectKey.isEmpty()) {
            return;
        }
        try {
            boolean deleted = objectStorageService.delete(objectKey);
            log.info("证书删除: objectKey={}, success={}", objectKey, deleted);
        } catch (Exception e) {
            log.error("证书删除失败: objectKey={}", objectKey, e);
        }
    }

    /**
     * 上传证据元数据 JSON 到 OSS
     *
     * @param certNo     证书编号
     * @param metadata   证据元数据 JSON
     * @return 元数据文件访问 URL
     */
    public String uploadEvidenceMetadata(String certNo, String metadata) {
        String objectKey = generateMetadataKey(certNo);
        byte[] content = metadata.getBytes(StandardCharsets.UTF_8);
        try (InputStream is = new ByteArrayInputStream(content)) {
            UploadResult result = objectStorageService.upload(
                    is, certNo + ".json", content.length, "application/json", "copyright-metadata");
            log.info("证据元数据上传成功: certNo={}, objectKey={}", certNo, result.getObjectKey());
            return result.getUrl();
        } catch (IOException e) {
            log.error("证据元数据上传失败: certNo={}", certNo, e);
            return null;
        }
    }

    /**
     * 计算文件的 SHA-256 哈希（用于链上存证）
     *
     * @param content 文件内容
     * @return SHA-256 哈希值
     */
    public String computeFileHash(byte[] content) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(content);
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("计算文件哈希失败", e);
            return null;
        }
    }

    /**
     * 生成证书 PDF 的 OSS 存储路径
     */
    private String generateCertificateKey(String certNo, String extension) {
        String datePath = LocalDate.now().format(DATE_FORMATTER);
        return String.format("%s/%s/%s.%s", basePath, datePath, certNo, extension);
    }

    /**
     * 生成二维码的 OSS 存储路径
     */
    private String generateQrKey(String certNo, String extension) {
        String datePath = LocalDate.now().format(DATE_FORMATTER);
        return String.format("%s/qr/%s/%s.%s", basePath, datePath, certNo, extension);
    }

    /**
     * 生成元数据 JSON 的 OSS 存储路径
     */
    private String generateMetadataKey(String certNo) {
        String datePath = LocalDate.now().format(DATE_FORMATTER);
        return String.format("%s/metadata/%s/%s.json", basePath, datePath, certNo);
    }
}
