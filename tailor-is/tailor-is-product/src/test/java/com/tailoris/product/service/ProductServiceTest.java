package com.tailoris.product.service;

import com.tailoris.common.constant.RedisKeyPrefix;
import com.tailoris.common.exception.BusinessException;
import com.tailoris.product.dto.ProductCreateRequest;
import com.tailoris.product.dto.ProductQueryRequest;
import com.tailoris.product.dto.ProductUpdateRequest;
import com.tailoris.product.entity.Product;
import com.tailoris.product.mapper.ProductMapper;
import com.tailoris.product.mapper.ProductSkuMapper;
import com.tailoris.product.mapper.ProductAttributeMapper;
import com.tailoris.product.mapper.ProductTagMappingMapper;
import com.tailoris.product.mapper.ProductReviewMapper;
import com.tailoris.product.mapper.ProductFavoriteMapper;
import com.tailoris.product.service.impl.ProductServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ProductService 补充单元测试 - TEST-P2-01.
 *
 * <p>覆盖 ProductServiceImplTest 中 TODO 待补充的场景：</p>
 * <ul>
 *   <li>商品搜索/筛选/排序测试</li>
 *   <li>商品上下架测试</li>
 *   <li>缓存穿透/击穿防护测试</li>
 *   <li>商品审核流程测试</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ProductService 补充单元测试")
class ProductServiceTest {

    @Mock private ProductMapper productMapper;
    @Mock private ProductSkuMapper productSkuMapper;
    @Mock private ProductAttributeMapper productAttributeMapper;
    @Mock private ProductTagMappingMapper productTagMappingMapper;
    @Mock private ProductReviewMapper productReviewMapper;
    @Mock private ProductFavoriteMapper productFavoriteMapper;
    @Mock private StringRedisTemplate stringRedisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;
    @Mock private com.tailoris.common.lock.DistributedLock distributedLock;

    @Spy private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private ProductServiceImpl productService;

    // ============================================================
    // 商品搜索/筛选/排序
    // ============================================================

    @Test
    @DisplayName("查询商品列表 - 按关键词搜索")
    void listProducts_ByKeyword() {
        Product product = buildProduct(1L, "修身西装");
        ProductQueryRequest request = new ProductQueryRequest();
        request.setKeyword("西装");
        request.setPageNum(1);
        request.setPageSize(10);

        @SuppressWarnings("unchecked")
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<Product> mockPage = mock(com.baomidou.mybatisplus.extension.plugins.pagination.Page.class);
        when(mockPage.getRecords()).thenReturn(List.of(product));
        when(mockPage.getTotal()).thenReturn(1L);
        when(productMapper.selectPage(any(), any())).thenReturn(mockPage);

        com.baomidou.mybatisplus.extension.plugins.pagination.Page<Product> result = productService.listProducts(request);

        assertNotNull(result);
        assertEquals(1L, result.getTotal());
        assertEquals("修身西装", result.getRecords().get(0).getName());
    }

    @Test
    @DisplayName("查询商品列表 - 按分类筛选")
    void listProducts_ByCategory() {
        Product product = buildProduct(1L, "衬衫");
        ProductQueryRequest request = new ProductQueryRequest();
        request.setCategoryId(10L);
        request.setPageNum(1);
        request.setPageSize(10);

        @SuppressWarnings("unchecked")
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<Product> mockPage = mock(com.baomidou.mybatisplus.extension.plugins.pagination.Page.class);
        when(mockPage.getRecords()).thenReturn(List.of(product));
        when(mockPage.getTotal()).thenReturn(1L);
        when(productMapper.selectPage(any(), any())).thenReturn(mockPage);

        com.baomidou.mybatisplus.extension.plugins.pagination.Page<Product> result = productService.listProducts(request);

        assertNotNull(result);
        assertEquals(1, result.getRecords().size());
    }

    @Test
    @DisplayName("查询商品列表 - 按状态筛选")
    void listProducts_ByStatus() {
        Product product = buildProduct(1L, "上架商品");
        product.setStatus(1);
        ProductQueryRequest request = new ProductQueryRequest();
        request.setStatus(1);
        request.setPageNum(1);
        request.setPageSize(10);

        @SuppressWarnings("unchecked")
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<Product> mockPage = mock(com.baomidou.mybatisplus.extension.plugins.pagination.Page.class);
        when(mockPage.getRecords()).thenReturn(List.of(product));
        when(mockPage.getTotal()).thenReturn(1L);
        when(productMapper.selectPage(any(), any())).thenReturn(mockPage);

        com.baomidou.mybatisplus.extension.plugins.pagination.Page<Product> result = productService.listProducts(request);

        assertNotNull(result);
        assertEquals(1, result.getRecords().size());
    }

    @Test
    @DisplayName("查询商品列表 - 无结果")
    void listProducts_NoResults() {
        ProductQueryRequest request = new ProductQueryRequest();
        request.setKeyword("不存在的商品");
        request.setPageNum(1);
        request.setPageSize(10);

        @SuppressWarnings("unchecked")
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<Product> mockPage = mock(com.baomidou.mybatisplus.extension.plugins.pagination.Page.class);
        when(mockPage.getRecords()).thenReturn(Collections.emptyList());
        when(mockPage.getTotal()).thenReturn(0L);
        when(productMapper.selectPage(any(), any())).thenReturn(mockPage);

        com.baomidou.mybatisplus.extension.plugins.pagination.Page<Product> result = productService.listProducts(request);

        assertEquals(0L, result.getTotal());
    }

    @Test
    @DisplayName("按商品类型查询")
    void getProductByType_Success() {
        Product product = buildProduct(1L, "定制西装");
        ProductQueryRequest request = new ProductQueryRequest();
        request.setPageNum(1);
        request.setPageSize(10);

        @SuppressWarnings("unchecked")
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<Product> mockPage = mock(com.baomidou.mybatisplus.extension.plugins.pagination.Page.class);
        when(mockPage.getRecords()).thenReturn(List.of(product));
        when(mockPage.getTotal()).thenReturn(1L);
        when(productMapper.selectPage(any(), any())).thenReturn(mockPage);

        com.baomidou.mybatisplus.extension.plugins.pagination.Page<Product> result = productService.getProductByType(1, request);

        assertNotNull(result);
        assertEquals(1, result.getRecords().size());
    }

    // ============================================================
    // 商品上下架
    // ============================================================

    @Test
    @DisplayName("上架商品 - 成功")
    void updateProductStatus_OnSale() {
        Long productId = 1L;
        Product product = buildProduct(productId, "待上架商品");
        product.setStatus(0);
        when(productMapper.selectById(productId)).thenReturn(product);
        when(productMapper.updateById(any(Product.class))).thenReturn(1);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(stringRedisTemplate.hasKey(anyString())).thenReturn(false);
        when(stringRedisTemplate.delete(anyString())).thenReturn(true);

        productService.updateProductStatus(productId, 1);

        verify(productMapper).updateById((Product) argThat(p -> p.getStatus() != null && p.getStatus() == 1));
    }

    @Test
    @DisplayName("下架商品 - 成功")
    void updateProductStatus_OffSale() {
        Long productId = 1L;
        Product product = buildProduct(productId, "已上架商品");
        product.setStatus(1);
        when(productMapper.selectById(productId)).thenReturn(product);
        when(productMapper.updateById(any(Product.class))).thenReturn(1);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(stringRedisTemplate.hasKey(anyString())).thenReturn(false);
        when(stringRedisTemplate.delete(anyString())).thenReturn(true);

        productService.updateProductStatus(productId, 0);

        verify(productMapper).updateById((Product) argThat(p -> p.getStatus() != null && p.getStatus() == 0));
    }

    @Test
    @DisplayName("上架商品 - 商品不存在")
    void updateProductStatus_NotFound() {
        when(productMapper.selectById(999L)).thenReturn(null);

        assertThrows(BusinessException.class, () -> productService.updateProductStatus(999L, 1));
    }

    // ============================================================
    // 缓存穿透/击穿防护
    // ============================================================

    @Test
    @DisplayName("获取商品详情 - 缓存穿透保护（空值缓存）")
    void getProductDetail_CachePenetrationProtection() {
        Long productId = 999L;
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(RedisKeyPrefix.PRODUCT + "detail:" + productId)).thenReturn(null);
        when(productMapper.selectById(productId)).thenReturn(null);
        when(distributedLock.executeWithLock(anyString(), anyLong(), anyLong(), any(TimeUnit.class), any()))
                .thenAnswer(invocation -> {
                    java.util.function.Supplier<?> task = invocation.getArgument(4);
                    return task.get();
                });

        assertThrows(BusinessException.class, () -> productService.getProductDetail(productId));
        // 验证空值被缓存以防止缓存穿透
        verify(valueOperations).set(eq(RedisKeyPrefix.PRODUCT + "detail:" + productId), eq("NULL"), anyLong(), any(TimeUnit.class));
    }

    @Test
    @DisplayName("更新商品 - 清除缓存")
    void updateProduct_CacheInvalidation() {
        Long productId = 1L;
        ProductUpdateRequest request = new ProductUpdateRequest();
        request.setName("更新商品");
        request.setCategoryId(10L);
        request.setProductType(1);
        request.setMainImage("/images/updated.jpg");
        request.setDescription("更新后的描述");

        Product existing = buildProduct(productId, "原商品");
        when(productMapper.selectById(productId)).thenReturn(existing);
        when(productMapper.updateById(any(Product.class))).thenReturn(1);
        when(stringRedisTemplate.delete(anyString())).thenReturn(true);

        productService.updateProduct(productId, request);

        verify(stringRedisTemplate).delete(RedisKeyPrefix.PRODUCT + "detail:" + productId);
    }

    // ============================================================
    // 商品审核流程
    // ============================================================

    @Test
    @DisplayName("审核商品 - 通过")
    void auditProduct_Approve() {
        Long productId = 1L;
        Product product = buildProduct(productId, "待审核商品");
        product.setAuditStatus(0);
        when(productMapper.selectById(productId)).thenReturn(product);
        when(productMapper.updateById(any(Product.class))).thenReturn(1);
        when(stringRedisTemplate.delete(anyString())).thenReturn(true);

        productService.auditProduct(productId, 2, "审核通过", 1L);

        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(productMapper).updateById(captor.capture());
        assertEquals(2, captor.getValue().getAuditStatus());
    }

    @Test
    @DisplayName("审核商品 - 驳回")
    void auditProduct_Reject() {
        Long productId = 1L;
        Product product = buildProduct(productId, "待审核商品");
        product.setAuditStatus(0);
        when(productMapper.selectById(productId)).thenReturn(product);
        when(productMapper.updateById(any(Product.class))).thenReturn(1);
        when(stringRedisTemplate.delete(anyString())).thenReturn(true);

        productService.auditProduct(productId, 3, "图片违规", 1L);

        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(productMapper).updateById(captor.capture());
        assertEquals(3, captor.getValue().getAuditStatus());
    }

    @Test
    @DisplayName("审核商品 - 商品不存在")
    void auditProduct_NotFound() {
        when(productMapper.selectById(999L)).thenReturn(null);

        assertThrows(BusinessException.class, () -> productService.auditProduct(999L, 2, "通过", 1L));
    }

    // ============================================================
    // 商品删除
    // ============================================================

    @Test
    @DisplayName("删除商品 - 成功")
    void deleteProduct_Success() {
        Long productId = 1L;
        Product product = buildProduct(productId, "下架商品");
        product.setStatus(0);
        when(productMapper.selectById(productId)).thenReturn(product);
        when(productTagMappingMapper.delete(any())).thenReturn(1);
        when(productAttributeMapper.delete(any())).thenReturn(1);
        when(productSkuMapper.delete(any())).thenReturn(1);
        when(productReviewMapper.softDeleteByProductId(productId)).thenReturn(0);
        when(productFavoriteMapper.softDeleteByProductId(productId)).thenReturn(0);
        when(productMapper.deleteById(productId)).thenReturn(1);
        when(stringRedisTemplate.delete(anyString())).thenReturn(true);

        productService.deleteProduct(productId);

        verify(productMapper).deleteById(productId);
    }

    @Test
    @DisplayName("删除商品 - 上架商品不能删除")
    void deleteProduct_OnSaleCannotDelete() {
        Long productId = 1L;
        Product product = buildProduct(productId, "上架商品");
        product.setStatus(1);
        when(productMapper.selectById(productId)).thenReturn(product);

        assertThrows(BusinessException.class, () -> productService.deleteProduct(productId));
        verify(productMapper, never()).deleteById(anyLong());
    }

    // ============================================================
    // 辅助方法
    // ============================================================

    private Product buildProduct(Long id, String name) {
        Product product = new Product();
        product.setId(id);
        product.setMerchantId(1L);
        product.setShopId(1L);
        product.setCategoryId(10L);
        product.setProductType(1);
        product.setName(name);
        product.setMainImage("/images/test.jpg");
        product.setStatus(1);
        product.setAuditStatus(2);
        product.setViewCount(0);
        product.setSaleCount(0);
        product.setCommentCount(0);
        product.setFavorableRate(new BigDecimal("100.00"));
        return product;
    }
}