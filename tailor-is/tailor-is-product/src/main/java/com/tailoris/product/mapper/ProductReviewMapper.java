package com.tailoris.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tailoris.product.entity.ProductReview;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ProductReviewMapper extends BaseMapper<ProductReview> {

    /**
     * 增加有用计数.
     */
    @Update("UPDATE product_review SET helpful_count = helpful_count + 1 WHERE id = #{id}")
    int incrementHelpfulCount(Long id);

    /**
     * 增加举报计数.
     */
    @Update("UPDATE product_review SET report_count = report_count + 1 WHERE id = #{id}")
    int incrementReportCount(Long id);

    /**
     * 🔒 PRD-007: 根据商品ID软删除评价.
     *
     * @param productId 商品ID
     * @return 影响行数
     */
    @Update("UPDATE product_review SET deleted = 1, update_time = NOW() WHERE product_id = #{productId} AND deleted = 0")
    int softDeleteByProductId(Long productId);
}
