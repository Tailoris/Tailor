package com.tailoris.product.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 评价搜索请求 DTO - PRD-009.
 *
 * @author Tailor IS Team
 * @since 1.0.0
 */
@Data
public class ReviewSearchRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 商品ID */
    private Long productId;

    /** 商家ID */
    private Long merchantId;

    /** 评分筛选（1-5） */
    private Integer minRating;
    private Integer maxRating;

    /** 是否仅显示有图评价 */
    private Boolean withImagesOnly;

    /** 是否仅显示精选 */
    private Boolean featuredOnly;

    /** 标签筛选 */
    private List<String> tags;

    /** 排序：new=最新 hot=最热 */
    private String sortBy = "new";

    private Integer pageNum = 1;
    private Integer pageSize = 20;
}
