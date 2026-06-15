package com.tailoris.product.search;

import com.tailoris.product.dto.ProductSearchRequest;
import com.tailoris.product.dto.ProductSearchResult;
import com.tailoris.product.entity.Product;

/**
 * 商品搜索抽象接口 - PRD-005.
 *
 * <p>双实现：</p>
 * <ul>
 *   <li>{@link ElasticsearchProductSearch}: ES 搜索引擎（生产推荐）</li>
 *   <li>{@link DatabaseProductSearch}: 数据库 LIKE 搜索（开发/降级）</li>
 * </ul>
 *
 * @author Tailor IS Team
 * @since 1.0.0
 */
public interface ProductSearchEngine {

    /**
     * 搜索商品.
     */
    ProductSearchResult search(ProductSearchRequest request);

    /**
     * 索引/重建商品索引.
     */
    boolean index(Product product);

    /**
     * 批量索引.
     */
    int indexBatch(java.util.List<Product> products);

    /**
     * 删除索引.
     */
    boolean delete(Long productId);

    /**
     * 全量重建索引.
     */
    int rebuildAll();
}
