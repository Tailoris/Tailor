package com.tailoris.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tailoris.product.entity.ProductFavorite;
import com.tailoris.product.mapper.ProductFavoriteMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ProductFavoriteServiceImpl 单元测试 - USR-008.
 *
 * @author Tailor IS Team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ProductFavoriteServiceImpl 商品收藏测试 (USR-008)")
class ProductFavoriteServiceImplTest {

    @Mock
    private ProductFavoriteMapper productFavoriteMapper;

    private ProductFavoriteServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ProductFavoriteServiceImpl(productFavoriteMapper);
    }

    @Test
    @DisplayName("addFavorite - 未收藏时成功插入")
    void addFavorite_New() {
        when(productFavoriteMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(productFavoriteMapper.insert(any(ProductFavorite.class))).thenReturn(1);

        service.addFavorite(1L, 100L);
        verify(productFavoriteMapper, times(1)).insert(any(ProductFavorite.class));
    }

    @Test
    @DisplayName("addFavorite - 已收藏时跳过")
    void addFavorite_AlreadyExists() {
        when(productFavoriteMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        service.addFavorite(1L, 100L);
        verify(productFavoriteMapper, never()).insert(any(ProductFavorite.class));
    }

    @Test
    @DisplayName("addFavorite - 并发重复插入抛 DuplicateKey 时吞掉异常")
    void addFavorite_DuplicateKeyException() {
        when(productFavoriteMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        doThrow(new DuplicateKeyException("duplicate")).when(productFavoriteMapper).insert(any(ProductFavorite.class));

        // 不应抛异常
        service.addFavorite(1L, 100L);
    }

    @Test
    @DisplayName("removeFavorite - 调用 delete")
    void removeFavorite() {
        when(productFavoriteMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(1);

        service.removeFavorite(1L, 100L);
        verify(productFavoriteMapper, times(1)).delete(any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("isFavorited - 已收藏返回 true")
    void isFavorited_True() {
        when(productFavoriteMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);
        assertTrue(service.isFavorited(1L, 100L));
    }

    @Test
    @DisplayName("isFavorited - 未收藏返回 false")
    void isFavorited_False() {
        when(productFavoriteMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        assertFalse(service.isFavorited(1L, 100L));
    }

    @Test
    @DisplayName("isFavorited - null 参数返回 false")
    void isFavorited_NullParams() {
        assertFalse(service.isFavorited(null, 100L));
        assertFalse(service.isFavorited(1L, null));
    }

    @Test
    @DisplayName("countByProduct - 返回收藏数")
    void countByProduct() {
        when(productFavoriteMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(10L);
        long count = service.countByProduct(100L);
        assertEquals(10L, count);
    }
}
