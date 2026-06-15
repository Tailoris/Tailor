package com.tailoris.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.tailoris.common.exception.BusinessException;
import com.tailoris.common.lock.DistributedLock;
import com.tailoris.common.lock.RedisDistributedLock;
import com.tailoris.product.entity.ProductSku;
import com.tailoris.product.mapper.ProductSkuMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ProductSkuServiceImpl 单元测试 - PRD-002 库存预扣减验证.
 *
 * @author Tailor IS Team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ProductSkuServiceImpl 库存预扣减测试 (PRD-002)")
class ProductSkuServiceImplTest {

    @Mock
    private ProductSkuMapper productSkuMapper;

    @Mock
    private RedisDistributedLock distributedLock;

    private ProductSkuServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ProductSkuServiceImpl(productSkuMapper, distributedLock);
    }

    @Test
    @DisplayName("updateStock - 库存充足时扣减成功")
    void updateStock_Success() {
        Long skuId = 1L;
        ProductSku sku = new ProductSku();
        sku.setId(skuId);
        sku.setStock(100);

        when(productSkuMapper.selectById(skuId)).thenReturn(sku);
        when(productSkuMapper.update(any(), any(UpdateWrapper.class))).thenReturn(1);

        // 模拟分布式锁直接执行 supplier
        when(distributedLock.executeWithLock(anyString(), any(), any(), any()))
                .thenAnswer(invocation -> {
                    Supplier<Boolean> supplier = invocation.getArgument(3);
                    return supplier.get();
                });

        boolean result = service.updateStock(skuId, 10, false);
        assertTrue(result);
        verify(productSkuMapper, times(1)).update(any(), any(UpdateWrapper.class));
    }

    @Test
    @DisplayName("updateStock - 库存不足时抛异常")
    void updateStock_Insufficient() {
        Long skuId = 1L;
        ProductSku sku = new ProductSku();
        sku.setId(skuId);
        sku.setStock(5);

        when(productSkuMapper.selectById(skuId)).thenReturn(sku);

        when(distributedLock.executeWithLock(anyString(), any(), any(), any()))
                .thenAnswer(invocation -> {
                    Supplier<Boolean> supplier = invocation.getArgument(3);
                    return supplier.get();
                });

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.updateStock(skuId, 10, false));
        assertTrue(ex.getMessage().contains("库存不足"));
        verify(productSkuMapper, never()).update(any(), any(UpdateWrapper.class));
    }

    @Test
    @DisplayName("updateStock - SKU不存在时抛异常")
    void updateStock_SkuNotFound() {
        Long skuId = 999L;
        when(productSkuMapper.selectById(skuId)).thenReturn(null);

        when(distributedLock.executeWithLock(anyString(), any(), any(), any()))
                .thenAnswer(invocation -> {
                    Supplier<Boolean> supplier = invocation.getArgument(3);
                    return supplier.get();
                });

        assertThrows(BusinessException.class, () -> service.updateStock(skuId, 10, false));
        verify(productSkuMapper, never()).update(any(), any(UpdateWrapper.class));
    }

    @Test
    @DisplayName("updateStock - 释放库存（increase=true）时直接累加")
    void updateStock_Increase() {
        Long skuId = 1L;
        ProductSku sku = new ProductSku();
        sku.setId(skuId);
        sku.setStock(10);

        when(productSkuMapper.selectById(skuId)).thenReturn(sku);
        when(productSkuMapper.update(any(), any(UpdateWrapper.class))).thenReturn(1);

        when(distributedLock.executeWithLock(anyString(), any(), any(), any()))
                .thenAnswer(invocation -> {
                    Supplier<Boolean> supplier = invocation.getArgument(3);
                    return supplier.get();
                });

        boolean result = service.updateStock(skuId, 50, true);
        assertTrue(result);

        ArgumentCaptor<UpdateWrapper> wrapperCaptor = ArgumentCaptor.forClass(UpdateWrapper.class);
        verify(productSkuMapper).update(any(), wrapperCaptor.capture());
        // 应包含 setSql("stock = stock + 50")
        String sqlSet = wrapperCaptor.getValue().getSqlSet();
        assertTrue(sqlSet.contains("stock + 50"));
    }

    @Test
    @DisplayName("updateStock - 乐观锁UPDATE rows=0时抛异常")
    void updateStock_OptimisticLockFail() {
        Long skuId = 1L;
        ProductSku sku = new ProductSku();
        sku.setId(skuId);
        sku.setStock(100);

        when(productSkuMapper.selectById(skuId)).thenReturn(sku);
        when(productSkuMapper.update(any(), any(UpdateWrapper.class))).thenReturn(0);  // 模拟并发冲突

        when(distributedLock.executeWithLock(anyString(), any(), any(), any()))
                .thenAnswer(invocation -> {
                    Supplier<Boolean> supplier = invocation.getArgument(3);
                    return supplier.get();
                });

        assertThrows(BusinessException.class, () -> service.updateStock(skuId, 10, false));
    }

    @Test
    @DisplayName("createSku - 默认值设置")
    void createSku_DefaultValues() {
        ProductSku sku = new ProductSku();
        sku.setSkuCode("SKU-001");
        when(productSkuMapper.insert(sku)).thenReturn(1);
        sku.setId(1L);

        Long id = service.createSku(100L, sku);
        assertEquals(100L, sku.getProductId());
        assertEquals(1, sku.getStatus());
        assertEquals(0, sku.getSalesCount());
    }

    @Test
    @DisplayName("updateSku - SKU不存在时抛异常")
    void updateSku_NotFound() {
        when(productSkuMapper.selectById(999L)).thenReturn(null);
        assertThrows(BusinessException.class, () -> service.updateSku(999L, new ProductSku()));
    }

    @Test
    @DisplayName("deleteSku - SKU不存在时抛异常")
    void deleteSku_NotFound() {
        when(productSkuMapper.selectById(999L)).thenReturn(null);
        assertThrows(BusinessException.class, () -> service.deleteSku(999L));
    }
}
