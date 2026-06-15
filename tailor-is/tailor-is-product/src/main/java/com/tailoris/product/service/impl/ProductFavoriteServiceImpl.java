package com.tailoris.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tailoris.common.exception.BusinessException;
import com.tailoris.product.entity.ProductFavorite;
import com.tailoris.product.mapper.ProductFavoriteMapper;
import com.tailoris.product.service.ProductFavoriteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 商品收藏服务实现 - USR-008.
 *
 * <p>使用 unique key (user_id, product_id) 防重复收藏，依赖数据库约束保证幂等。</p>
 *
 * @author Tailor IS Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductFavoriteServiceImpl implements ProductFavoriteService {

    private final ProductFavoriteMapper productFavoriteMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addFavorite(Long userId, Long productId) {
        if (userId == null || productId == null) {
            throw new BusinessException("用户ID与商品ID不能为空");
        }
        // 1. 检查是否已收藏
        if (isFavorited(userId, productId)) {
            log.debug("已收藏, 跳过: userId={}, productId={}", userId, productId);
            return;
        }
        // 2. 写入（依赖DB唯一约束）
        ProductFavorite favorite = new ProductFavorite();
        favorite.setUserId(userId);
        favorite.setProductId(productId);
        favorite.setCreateTime(LocalDateTime.now());
        favorite.setUpdateTime(LocalDateTime.now());
        try {
            productFavoriteMapper.insert(favorite);
            log.info("收藏成功: userId={}, productId={}", userId, productId);
        } catch (DuplicateKeyException e) {
            // 并发场景下的重复插入，吞掉异常
            log.debug("并发收藏, 已存在: userId={}, productId={}", userId, productId);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeFavorite(Long userId, Long productId) {
        LambdaQueryWrapper<ProductFavorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProductFavorite::getUserId, userId)
               .eq(ProductFavorite::getProductId, productId);
        int rows = productFavoriteMapper.delete(wrapper);
        log.info("取消收藏: userId={}, productId={}, rows={}", userId, productId, rows);
    }

    @Override
    public boolean isFavorited(Long userId, Long productId) {
        if (userId == null || productId == null) {
            return false;
        }
        LambdaQueryWrapper<ProductFavorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProductFavorite::getUserId, userId)
               .eq(ProductFavorite::getProductId, productId)
               .last("LIMIT 1");
        Long count = productFavoriteMapper.selectCount(wrapper);
        return count != null && count > 0;
    }

    @Override
    public Page<ProductFavorite> listFavorites(Long userId, int pageNum, int pageSize) {
        Page<ProductFavorite> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<ProductFavorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProductFavorite::getUserId, userId)
               .orderByDesc(ProductFavorite::getCreateTime);
        return productFavoriteMapper.selectPage(page, wrapper);
    }

    @Override
    public long countByProduct(Long productId) {
        LambdaQueryWrapper<ProductFavorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProductFavorite::getProductId, productId);
        return productFavoriteMapper.selectCount(wrapper);
    }
}
