package com.tailoris.product.service.impl;

import com.tailoris.common.exception.BusinessException;
import com.tailoris.common.oss.FileUploadService;
import com.tailoris.product.constant.ProductTypeConstants;
import com.tailoris.product.dto.CustomMeasurementRequest;
import com.tailoris.product.entity.CustomMeasurement;
import com.tailoris.product.entity.DigitalPattern;
import com.tailoris.product.entity.PatternDownloadToken;
import com.tailoris.product.entity.Product;
import com.tailoris.product.exception.ProductTypeMismatchException;
import com.tailoris.product.mapper.CustomMeasurementMapper;
import com.tailoris.product.mapper.DigitalPatternMapper;
import com.tailoris.product.mapper.PatternDownloadTokenMapper;
import com.tailoris.product.service.ProductTypeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 商品类型差异化服务实现 - PRD-008.
 *
 * <h3>差异化处理矩阵</h3>
 * <table>
 *   <tr><th>维度</th><th>实物 (1)</th><th>数字纸样 (2)</th><th>定制 (3)</th></tr>
 *   <tr><td>价格</td><td>SKU定价</td><td>统一价 + 设计费</td><td>基础价 + 工艺费</td></tr>
 *   <tr><td>库存</td><td>需要</td><td>不限量</td><td>按订单</td></tr>
 *   <tr><td>物流</td><td>需要</td><td>否（下载）</td><td>需要</td></tr>
 *   <tr><td>退款</td><td>支持</td><td>已下载不支持</td><td>生产前支持</td></tr>
 *   <tr><td>评价</td><td>支持</td><td>支持</td><td>支持</td></tr>
 * </table>
 *
 * @author Tailor IS Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductTypeServiceImpl implements ProductTypeService {

    private final DigitalPatternMapper digitalPatternMapper;
    private final PatternDownloadTokenMapper downloadTokenMapper;
    private final CustomMeasurementMapper customMeasurementMapper;
    private final FileUploadService fileUploadService;
    private final StringRedisTemplate stringRedisTemplate;

    /** 下载token默认有效期：7天 */
    private static final int DEFAULT_TOKEN_TTL_DAYS = 7;
    /** 默认最大下载次数：3次 */
    private static final int DEFAULT_MAX_DOWNLOAD = 3;
    /** token防重放：一次使用后立即失效 */
    private static final String DOWNLOAD_USED_KEY = "tailoris:pattern:download:used:";

    @Value("${tailoris.product.pattern.max-download:3}")
    private int maxDownloadCount;

    @Value("${tailoris.product.pattern.token-ttl-days:7}")
    private int tokenTtlDays;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createExtension(Product product, Object extensionData) {
        if (product == null || product.getProductType() == null) {
            throw new BusinessException("商品或商品类型不能为空");
        }
        int type = product.getProductType();

        if (type == ProductTypeConstants.DIGITAL_PATTERN) {
            // 数字纸样：上传文件 + 创建DigitalPattern记录
            if (!(extensionData instanceof MultipartFile)) {
                throw new BusinessException("数字纸样需要提供文件");
            }
            uploadDigitalPatternFile(product.getId(), (MultipartFile) extensionData);
        } else if (type == ProductTypeConstants.CUSTOM) {
            // 定制商品：预留接口用于工艺参数配置
            log.info("定制商品创建: productId={}, 无需文件", product.getId());
        } else if (type == ProductTypeConstants.PHYSICAL) {
            // 实物：无需扩展
            log.debug("实物商品无需扩展: productId={}", product.getId());
        } else {
            throw ProductTypeMismatchException.of(type, -1);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteExtension(Long productId, Integer productType) {
        if (productType == null) return;
        if (productType == ProductTypeConstants.DIGITAL_PATTERN) {
            // 删除数字纸样记录
            digitalPatternMapper.deleteByProductId(productId);
        }
        log.info("商品类型扩展已删除: productId={}, type={}", productId, productType);
    }

    // ============================================================
    // 数字纸样相关
    // ============================================================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PatternDownloadToken generateDownloadToken(Long userId, Long orderId, Long patternId) {
        DigitalPattern pattern = digitalPatternMapper.selectById(patternId);
        if (pattern == null) {
            throw new BusinessException("数字纸样不存在");
        }

        // 1. 生成 token（Base64编码的UUID + 时间戳）
        String token = generateSecureToken(userId, patternId);

        // 2. 计算过期时间
        int licenseDays = pattern.getLicenseDurationDays() == null ? 0 : pattern.getLicenseDurationDays();
        LocalDateTime expireTime = (licenseDays == 0)
                ? LocalDateTime.now().plusDays(tokenTtlDays)  // 永久授权但token有过期
                : LocalDateTime.now().plusDays(licenseDays);

        // 3. 写库
        PatternDownloadToken downloadToken = new PatternDownloadToken();
        downloadToken.setUserId(userId);
        downloadToken.setOrderId(orderId);
        downloadToken.setPatternId(patternId);
        downloadToken.setProductId(pattern.getProductId());
        downloadToken.setToken(token);
        downloadToken.setMaxDownloadCount(maxDownloadCount);
        downloadToken.setUsedCount(0);
        downloadToken.setExpireTime(expireTime);
        downloadToken.setCreateTime(LocalDateTime.now());
        downloadTokenMapper.insert(downloadToken);

        // 4. 同步缓存（防重放 + 提升查询性能）
        stringRedisTemplate.opsForValue().set(
                "tailoris:pattern:token:" + token,
                String.valueOf(downloadToken.getId()),
                java.time.Duration.between(LocalDateTime.now(), expireTime));

        log.info("数字纸样下载token生成: userId={}, patternId={}, expireAt={}",
                userId, patternId, expireTime);
        return downloadToken;
    }

    @Override
    public String downloadByToken(String token, String clientIp) {
        if (!StringUtils.hasText(token)) {
            throw new BusinessException("下载token不能为空");
        }

        // 1. 防止重放
        String usedKey = DOWNLOAD_USED_KEY + token;
        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(usedKey))) {
            throw new BusinessException("下载链接已失效，请重新获取");
        }

        // 2. 优先从Redis查（热数据）
        String cacheId = stringRedisTemplate.opsForValue().get("tailoris:pattern:token:" + token);
        PatternDownloadToken record = (cacheId != null)
                ? downloadTokenMapper.selectById(Long.valueOf(cacheId))
                : downloadTokenMapper.selectByToken(token);
        if (record == null) {
            throw new BusinessException("下载token不存在或已过期");
        }

        // 3. 校验
        if (record.getExpireTime() != null && record.getExpireTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException("下载token已过期");
        }
        if (record.getMaxDownloadCount() != null
                && record.getUsedCount() != null
                && record.getUsedCount() >= record.getMaxDownloadCount()) {
            throw new BusinessException("已达最大下载次数限制");
        }

        // 4. 消费（CAS 乐观更新）
        int rows = downloadTokenMapper.atomicConsume(record.getId());
        if (rows == 0) {
            // 并发场景：其他线程已消费
            throw new BusinessException("下载链接被其他请求占用，请重试");
        }
        stringRedisTemplate.opsForValue().set(usedKey, "1", 60, TimeUnit.SECONDS);

        // 5. 异步增加下载统计
        try {
            digitalPatternMapper.incrementDownloadCount(record.getPatternId());
        } catch (Exception e) {
            log.warn("下载统计失败: {}", e.getMessage());
        }

        // 6. 生成预签名URL（私有Bucket授权访问）
        DigitalPattern pattern = digitalPatternMapper.selectById(record.getPatternId());
        if (pattern == null) {
            throw new BusinessException("纸样文件不存在");
        }
        String url = fileUploadService.generatePresignedUrl(pattern.getFileKey(), 600);

        // 7. 审计日志
        log.info("数字纸样下载: userId={}, patternId={}, orderId={}, clientIp={}",
                record.getUserId(), record.getPatternId(), record.getOrderId(), clientIp);

        return url;
    }

    @Override
    public List<DigitalPattern> listOwnedPatterns(Long userId) {
        // 通过订单 + 下载token反查用户拥有的纸样
        return digitalPatternMapper.listOwnedByUser(userId);
    }

    // ============================================================
    // 定制商品相关
    // ============================================================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CustomMeasurement saveMeasurement(Long userId, Long orderId, Long productId,
                                              CustomMeasurementRequest request) {
        if (userId == null || orderId == null || productId == null) {
            throw new BusinessException("用户/订单/商品ID不能为空");
        }

        // 1. 数据合理性校验（参考GB/T 10000-1988）
        validateMeasurement(request);

        // 2. 持久化
        CustomMeasurement measurement = new CustomMeasurement();
        measurement.setUserId(userId);
        measurement.setOrderId(orderId);
        measurement.setProductId(productId);
        copyRequest(request, measurement);
        measurement.setCreateTime(LocalDateTime.now());
        measurement.setUpdateTime(LocalDateTime.now());
        customMeasurementMapper.insert(measurement);

        log.info("定制参数保存: userId={}, orderId={}, height={}, bust={}, waist={}",
                userId, orderId,
                request.getHeight(), request.getBust(), request.getWaist());
        return measurement;
    }

    @Override
    public CustomMeasurement getMeasurementByOrder(Long orderId) {
        return customMeasurementMapper.selectByOrderId(orderId);
    }

    // ============================================================
    // 私有方法
    // ============================================================

    /**
     * 上传数字纸样文件.
     */
    private void uploadDigitalPatternFile(Long productId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("纸样文件不能为空");
        }

        try {
            com.tailoris.common.oss.UploadResult upload = fileUploadService.upload(file, "pattern");
            DigitalPattern pattern = new DigitalPattern();
            pattern.setProductId(productId);
            pattern.setFileKey(upload.getObjectKey());
            pattern.setFileUrl(upload.getUrl());
            pattern.setFileSize(upload.getSize());
            pattern.setFileFormat(extractFormat(file.getOriginalFilename()));
            pattern.setDownloadCount(0);
            pattern.setPreviewCount(0);
            pattern.setCreateTime(LocalDateTime.now());
            pattern.setUpdateTime(LocalDateTime.now());
            digitalPatternMapper.insert(pattern);
            log.info("数字纸样文件上传: productId={}, fileKey={}", productId, upload.getObjectKey());
        } catch (IOException e) {
            log.error("数字纸样上传失败: productId={}", productId, e);
            throw new BusinessException("纸样文件上传失败");
        }
    }

    /**
     * 生成安全的下载token.
     */
    private String generateSecureToken(Long userId, Long patternId) {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[24];
        random.nextBytes(bytes);
        String raw = userId + ":" + patternId + ":" + UUID.randomUUID();
        return Base64.getUrlEncoder().withoutPadding().encodeToString(
                (raw + ":" + new String(bytes)).getBytes());
    }

    private String extractFormat(String filename) {
        if (filename == null) return "unknown";
        int dotIdx = filename.lastIndexOf('.');
        return dotIdx > 0 ? filename.substring(dotIdx + 1).toLowerCase() : "unknown";
    }

    private void validateMeasurement(CustomMeasurementRequest r) {
        if (r.getHeight() != null && (r.getHeight().doubleValue() < 100 || r.getHeight().doubleValue() > 250)) {
            throw new BusinessException("身高数据异常（100-250cm）");
        }
        if (r.getBust() != null && (r.getBust().doubleValue() < 60 || r.getBust().doubleValue() > 180)) {
            throw new BusinessException("胸围数据异常（60-180cm）");
        }
        if (r.getWaist() != null && (r.getWaist().doubleValue() < 50 || r.getWaist().doubleValue() > 180)) {
            throw new BusinessException("腰围数据异常（50-180cm）");
        }
    }

    private void copyRequest(CustomMeasurementRequest r, CustomMeasurement m) {
        m.setHeight(r.getHeight());
        m.setWeight(r.getWeight());
        m.setBust(r.getBust());
        m.setWaist(r.getWaist());
        m.setHip(r.getHip());
        m.setShoulder(r.getShoulder());
        m.setSleeveLength(r.getSleeveLength());
        m.setPantsLength(r.getPantsLength());
        m.setNeck(r.getNeck());
        m.setArm(r.getArm());
        m.setThigh(r.getThigh());
        m.setFitPreference(r.getFitPreference());
        m.setColorPreference(r.getColorPreference());
        m.setRemark(r.getRemark());
    }
}
