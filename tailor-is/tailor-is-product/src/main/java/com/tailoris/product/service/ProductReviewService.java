package com.tailoris.product.service;

import com.tailoris.product.dto.CreateReviewRequest;
import com.tailoris.product.dto.ReviewReplyRequest;
import com.tailoris.product.dto.ReviewSearchRequest;
import com.tailoris.product.entity.ProductReview;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

/**
 * 商品评价服务接口 - PRD-009.
 *
 * @author Tailor IS Team
 * @since 1.0.0
 */
public interface ProductReviewService {

    /**
     * 创建评价（含敏感词过滤、图片审核）.
     */
    Long createReview(Long userId, CreateReviewRequest request);

    /**
     * 商家回复.
     */
    void merchantReply(Long merchantId, Long reviewId, ReviewReplyRequest request);

    /**
     * 用户标记评价有用.
     */
    void markHelpful(Long userId, Long reviewId);

    /**
     * 举报评价.
     */
    void reportReview(Long userId, Long reviewId, String reason);

    /**
     * 商家精选/取消精选.
     */
    void featureReview(Long merchantId, Long reviewId, boolean featured);

    /**
     * 商品评价分页查询.
     */
    Page<ProductReview> listProductReviews(ReviewSearchRequest request);

    /**
     * 用户评价列表.
     */
    Page<ProductReview> listUserReviews(Long userId, int pageNum, int pageSize);

    /**
     * 根据商品ID分页查询评价.
     */
    Page<ProductReview> listReviewsByProduct(Long productId, int pageNum, int pageSize);

    /**
     * 删除评价.
     */
    void deleteReview(Long id);

    /**
     * 查询待审核评价列表.
     */
    Page<ProductReview> listPendingReviews(int pageNum, int pageSize);

    /**
     * 审核评价.
     */
    void auditReview(Long id, int status);
}
