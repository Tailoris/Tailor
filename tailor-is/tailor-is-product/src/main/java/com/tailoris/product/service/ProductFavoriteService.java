package com.tailoris.product.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tailoris.product.entity.ProductFavorite;

/**
 * 商品收藏服务接口 - USR-008.
 *
 * @author Tailor IS Team
 * @since 1.0.0
 */
public interface ProductFavoriteService {

    /**
     * 添加收藏.
     */
    void addFavorite(Long userId, Long productId);

    /**
     * 取消收藏.
     */
    void removeFavorite(Long userId, Long productId);

    /**
     * 判断是否已收藏.
     */
    boolean isFavorited(Long userId, Long productId);

    /**
     * 用户收藏列表.
     */
    Page<ProductFavorite> listFavorites(Long userId, int pageNum, int pageSize);

    /**
     * 商品的收藏数.
     */
    long countByProduct(Long productId);
}
