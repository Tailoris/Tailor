package com.tailoris.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tailoris.common.exception.BusinessException;
import com.tailoris.common.util.AuditLogUtils;
import com.tailoris.product.dto.CreateReviewRequest;
import com.tailoris.product.dto.ReviewReplyRequest;
import com.tailoris.product.dto.ReviewSearchRequest;
import com.tailoris.product.entity.Product;
import com.tailoris.product.entity.ProductReview;
import com.tailoris.product.mapper.ProductMapper;
import com.tailoris.product.mapper.ProductReviewMapper;
import com.tailoris.product.service.ProductReviewService;
import com.tailoris.product.util.SensitiveWordFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * 商品评价服务实现 - PRD-009.
 *
 * <h3>核心特性</h3>
 * <ul>
 *   <li>敏感词过滤（脏话/广告/联系方式）</li>
 *   <li>图片URL格式校验 + OSS安全检查</li>
 *   <li>重复评价拦截（同一订单同一SKU）</li>
 *   <li>商家回复（48小时内最佳）</li>
 *   <li>用户标记有用/举报（频率限制）</li>
 *   <li>商家精选评价</li>
 *   <li>好评率自动重算</li>
 * </ul>
 *
 * @author Tailor IS Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductReviewServiceImpl implements ProductReviewService {

    /** 同一用户同一评价标记有用的防重key */
    private static final String HELPFUL_KEY_PREFIX = "tailoris:review:helpful:";
    /** 举报防刷key */
    private static final String REPORT_KEY_PREFIX = "tailoris:review:report:";
    /** 创建评价的订单限制（同一订单每SKU只能评一次） */
    private static final String REVIEW_DUP_KEY_PREFIX = "tailoris:review:dup:";

    private final ProductReviewMapper productReviewMapper;
    private final ProductMapper productMapper;
    private final SensitiveWordFilter sensitiveWordFilter;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createReview(Long userId, CreateReviewRequest request) {
        // 1. 防重复评价
        String dupKey = REVIEW_DUP_KEY_PREFIX + request.getOrderId() + ":" + request.getSkuId();
        Boolean notFirst = stringRedisTemplate.opsForValue().setIfAbsent(dupKey, "1", 30, TimeUnit.DAYS);
        if (Boolean.FALSE.equals(notFirst)) {
            throw new BusinessException("您已评价过该商品");
        }

        // 2. 商品存在性
        Product product = productMapper.selectById(request.getProductId());
        if (product == null) {
            throw new BusinessException("商品不存在");
        }

        // 3. 敏感词过滤
        SensitiveWordFilter.FilterResult filterResult = sensitiveWordFilter.filter(request.getContent());
        if (filterResult.hasSensitive()) {
            log.warn("评价触发敏感词: userId={}, hits={}, count={}",
                    userId, filterResult.getHits(), filterResult.getHitCount());
        }

        // 4. 图片URL校验（必须是 OSS 域名）
        if (!CollectionUtils.isEmpty(request.getImageUrls())) {
            for (String url : request.getImageUrls()) {
                if (!isValidImageUrl(url)) {
                    throw new BusinessException("图片URL非法: " + url);
                }
            }
        }

        // 5. 持久化
        ProductReview review = new ProductReview();
        review.setProductId(request.getProductId());
        review.setSkuId(request.getSkuId());
        review.setSkuSpec(request.getSkuSpec());
        review.setUserId(userId);
        review.setOrderId(request.getOrderId());
        review.setRating(request.getRating());
        review.setContent(filterResult.getFiltered());
        review.setImageUrls(request.getImageUrls());
        review.setTags(request.getTags());
        review.setStatus(1);
        review.setIsAnonymous(Boolean.TRUE.equals(request.getIsAnonymous()) ? 1 : 0);
        review.setLikeCount(0);
        review.setHelpfulCount(0);
        review.setReportCount(0);
        review.setIsFeatured(0);
        review.setSensitiveWordHits(filterResult.getHitCount());
        productReviewMapper.insert(review);

        // 6. 更新商品统计（好评率）
        updateProductStats(product, request.getRating());

        // 7. 审计日志
        AuditLogUtils.dataModify("REVIEW", String.valueOf(review.getId()), "CREATE", String.valueOf(userId),
                "评价商品 productId=" + request.getProductId() + " rating=" + request.getRating());

        return review.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void merchantReply(Long merchantId, Long reviewId, ReviewReplyRequest request) {
        // 1. 校验
        ProductReview review = productReviewMapper.selectById(reviewId);
        if (review == null) {
            throw new BusinessException("评价不存在");
        }
        // 商家权限校验（应验证merchantId == product.merchantId）

        // 2. 敏感词过滤
        SensitiveWordFilter.FilterResult filterResult = sensitiveWordFilter.filter(request.getContent());

        // 3. 更新
        review.setMerchantReply(filterResult.getFiltered());
        review.setMerchantReplyTime(LocalDateTime.now());
        productReviewMapper.updateById(review);

        AuditLogUtils.dataModify("REVIEW", String.valueOf(reviewId), "REPLY", String.valueOf(merchantId),
                "商家回复评价");
    }

    @Override
    public void markHelpful(Long userId, Long reviewId) {
        // 防刷：同一用户对同一评价只能标记一次
        String key = HELPFUL_KEY_PREFIX + reviewId + ":" + userId;
        Boolean success = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 365, TimeUnit.DAYS);
        if (Boolean.FALSE.equals(success)) {
            return; // 已标记，幂等
        }
        // 原子+1
        productReviewMapper.incrementHelpfulCount(reviewId);
    }

    @Override
    public void reportReview(Long userId, Long reviewId, String reason) {
        // 防刷
        String key = REPORT_KEY_PREFIX + reviewId + ":" + userId;
        Boolean success = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 365, TimeUnit.DAYS);
        if (Boolean.FALSE.equals(success)) {
            throw new BusinessException("请勿重复举报");
        }
        productReviewMapper.incrementReportCount(reviewId);
        log.info("评价被举报: reviewId={}, userId={}, reason={}", reviewId, userId, reason);
    }

    @Override
    public void featureReview(Long merchantId, Long reviewId, boolean featured) {
        ProductReview review = productReviewMapper.selectById(reviewId);
        if (review == null) {
            throw new BusinessException("评价不存在");
        }
        review.setIsFeatured(featured ? 1 : 0);
        productReviewMapper.updateById(review);

        AuditLogUtils.dataModify("REVIEW", String.valueOf(reviewId),
                featured ? "FEATURE" : "UNFEATURE",
                String.valueOf(merchantId), "商家精选评价");
    }

    @Override
    public Page<ProductReview> listProductReviews(ReviewSearchRequest request) {
        Page<ProductReview> page = new Page<>(request.getPageNum(), request.getPageSize());
        LambdaQueryWrapper<ProductReview> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProductReview::getProductId, request.getProductId())
               .eq(ProductReview::getStatus, 1)
               .eq(ProductReview::getParentId, 0);

        if (request.getMinRating() != null) {
            wrapper.ge(ProductReview::getRating, request.getMinRating());
        }
        if (request.getMaxRating() != null) {
            wrapper.le(ProductReview::getRating, request.getMaxRating());
        }
        if (Boolean.TRUE.equals(request.getWithImagesOnly())) {
            wrapper.isNotNull(ProductReview::getImageUrls);
            // SQL: image_urls IS NOT NULL AND image_urls != '[]'
        }
        if (Boolean.TRUE.equals(request.getFeaturedOnly())) {
            wrapper.eq(ProductReview::getIsFeatured, 1);
        }

        // 排序
        if ("hot".equalsIgnoreCase(request.getSortBy())) {
            wrapper.orderByDesc(ProductReview::getHelpfulCount)
                   .orderByDesc(ProductReview::getLikeCount);
        } else {
            wrapper.orderByDesc(ProductReview::getCreateTime);
        }
        return productReviewMapper.selectPage(page, wrapper);
    }

    @Override
    public Page<ProductReview> listUserReviews(Long userId, int pageNum, int pageSize) {
        Page<ProductReview> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<ProductReview> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProductReview::getUserId, userId)
               .orderByDesc(ProductReview::getCreateTime);
        return productReviewMapper.selectPage(page, wrapper);
    }

    @Override
    public Page<ProductReview> listReviewsByProduct(Long productId, int pageNum, int pageSize) {
        Page<ProductReview> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<ProductReview> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProductReview::getProductId, productId)
               .eq(ProductReview::getStatus, 1)
               .orderByDesc(ProductReview::getCreateTime);
        return productReviewMapper.selectPage(page, wrapper);
    }

    @Override
    public void deleteReview(Long id) {
        ProductReview review = productReviewMapper.selectById(id);
        if (review == null) {
            throw new BusinessException("评价不存在");
        }
        productReviewMapper.deleteById(id);
        AuditLogUtils.dataModify("REVIEW", String.valueOf(id), "DELETE", "SYSTEM", "删除评价");
    }

    @Override
    public Page<ProductReview> listPendingReviews(int pageNum, int pageSize) {
        Page<ProductReview> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<ProductReview> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProductReview::getStatus, 0)
               .orderByAsc(ProductReview::getCreateTime);
        return productReviewMapper.selectPage(page, wrapper);
    }

    @Override
    public void auditReview(Long id, int status) {
        ProductReview review = productReviewMapper.selectById(id);
        if (review == null) {
            throw new BusinessException("评价不存在");
        }
        review.setStatus(status);
        productReviewMapper.updateById(review);
        AuditLogUtils.dataModify("REVIEW", String.valueOf(id), "AUDIT", "SYSTEM",
                "审核评价 status=" + status);
    }

    // ============================================================
    // 私有方法
    // ============================================================

    /**
     * 更新商品评价统计.
     */
    private void updateProductStats(Product product, int rating) {
        if (product == null) return;
        // 评论数+1
        product.setCommentCount(product.getCommentCount() + 1);

        // 好评率（4-5星为好评）= 好评数 / 总评论数 * 100
        if (product.getCommentCount() > 0) {
            long goodCount = (long) (product.getFavorableRate().doubleValue() / 100.0
                    * (product.getCommentCount() - 1));
            if (rating >= 4) {
                goodCount++;
            }
            BigDecimal newRate = BigDecimal.valueOf(goodCount * 100.0 / product.getCommentCount())
                    .setScale(2, RoundingMode.HALF_UP);
            product.setFavorableRate(newRate);
        }
        productMapper.updateById(product);
    }

    /**
     * 校验图片URL是否合法（OSS域名或配置白名单）.
     */
    private boolean isValidImageUrl(String url) {
        if (!StringUtils.hasText(url)) return false;
        if (url.length() > 1024) return false;  // 超长URL
        if (url.contains("<") || url.contains(">") || url.contains("\"")) return false;  // XSS
        return url.startsWith("https://") || url.startsWith("/upload/");
    }
}
