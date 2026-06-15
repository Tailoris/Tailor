package com.tailoris.product.dto;

import com.tailoris.product.entity.Product;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 商品搜索结果 - PRD-005.
 *
 * @author Tailor IS Team
 * @since 1.0.0
 */
@Data
public class ProductSearchResult implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 商品列表 */
    private List<Product> products;

    /** 总数 */
    private Long total;

    /** 当前页 */
    private Integer pageNum;

    /** 每页大小 */
    private Integer pageSize;

    /** 总页数 */
    private Integer totalPages;

    /** 是否来自搜索引擎（false=数据库搜索） */
    private Boolean fromEs;

    /** 搜索耗时（毫秒） */
    private Long costMs;

    /** 搜索建议词（搜索词无结果时） */
    private List<String> suggestions;
}
