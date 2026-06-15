package com.tailoris.product.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品搜索请求 DTO - PRD-005.
 *
 * <p>支持多维度筛选：关键词、类目、价格区间、店铺、标签、排序、分页。</p>
 *
 * @author Tailor IS Team
 * @since 1.0.0
 */
@Data
public class ProductSearchRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 关键词（搜索商品名/描述/标签） */
    private String keyword;

    /** 商品类型：1=实物 2=数字纸样 3=定制 */
    private Integer productType;

    /** 一级类目ID */
    private Long categoryId;

    /** 店铺ID */
    private Long shopId;

    /** 商家ID */
    private Long merchantId;

    /** 最低价 */
    private BigDecimal priceMin;

    /** 最高价 */
    private BigDecimal priceMax;

    /** 标签ID列表 */
    private java.util.List<Long> tagIds;

    /** 是否仅显示有货（库存>0） */
    private Boolean inStockOnly;

    /** 排序字段：sales/salesCount/price/new/created */
    private String sortBy = "new";

    /** 升降序：asc/desc */
    private String order = "desc";

    /** 页码（从1开始） */
    private Integer pageNum = 1;

    /** 每页大小 */
    private Integer pageSize = 20;

    public Integer offset() {
        return (pageNum - 1) * pageSize;
    }
}
