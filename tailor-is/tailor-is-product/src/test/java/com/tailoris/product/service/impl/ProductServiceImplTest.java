package com.tailoris.product.service.impl;

import com.tailoris.common.constant.RedisKeyPrefix;
import com.tailoris.common.exception.BusinessException;
import com.tailoris.product.dto.ProductCreateRequest;
import com.tailoris.product.dto.ProductUpdateRequest;
import com.tailoris.product.entity.Product;
import com.tailoris.product.entity.ProductAttribute;
import com.tailoris.product.entity.ProductSku;
import com.tailoris.product.entity.ProductTagMapping;
import com.tailoris.product.mapper.ProductAttributeMapper;
import com.tailoris.product.mapper.ProductFavoriteMapper;
import com.tailoris.product.mapper.ProductMapper;
import com.tailoris.product.mapper.ProductReviewMapper;
import com.tailoris.product.mapper.ProductSkuMapper;
import com.tailoris.product.mapper.ProductTagMappingMapper;
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

@DisplayName("ProductServiceImpl 测试")
@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private ProductMapper productMapper;

    @Mock
    private ProductSkuMapper productSkuMapper;

    @Mock
    private ProductAttributeMapper productAttributeMapper;

    @Mock
    private ProductTagMappingMapper productTagMappingMapper;

    @Mock
    private ProductReviewMapper productReviewMapper;

    @Mock
    private ProductFavoriteMapper productFavoriteMapper;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private com.tailoris.common.lock.DistributedLock distributedLock;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private ProductServiceImpl productService;

    @Test
    @DisplayName("创建产品成功")
    void testCreateProduct_Success() {
        ProductCreateRequest request = buildProductCreateRequest();

        when(distributedLock.tryLock(anyString(), anyLong(), any(TimeUnit.class))).thenReturn("token-123");
        when(productMapper.selectCount(any())).thenReturn(0L);
        when(productMapper.insert(any(Product.class))).thenAnswer(invocation -> {
            Product p = invocation.getArgument(0);
            p.setId(1L);
            return 1;
        });

        Long productId = productService.createProduct(request);

        assertNotNull(productId);
        assertEquals(1L, productId);
        verify(productMapper).insert(any(Product.class));
        verify(productSkuMapper, never()).insertBatchSomeColumn(any());
        verify(productAttributeMapper, never()).insert(any(ProductAttribute.class));
        verify(productTagMappingMapper, never()).insert(any(ProductTagMapping.class));
    }

    @Test
    @DisplayName("创建带SKU的产品成功")
    void testCreateProduct_WithSkus() {
        ProductCreateRequest request = buildProductCreateRequest();

        ProductCreateRequest.SkuCreateRequest sku1 = new ProductCreateRequest.SkuCreateRequest();
        sku1.setSkuCode("SKU-001");
        sku1.setPrice(new BigDecimal("129.00"));
        sku1.setOriginalPrice(new BigDecimal("199.00"));
        sku1.setCostPrice(new BigDecimal("89.00"));
        sku1.setStock(100);
        sku1.setStatus(1);

        ProductCreateRequest.SkuCreateRequest sku2 = new ProductCreateRequest.SkuCreateRequest();
        sku2.setSkuCode("SKU-002");
        sku2.setPrice(new BigDecimal("159.00"));
        sku2.setStock(50);
        sku2.setStatus(1);

        request.setSkus(Arrays.asList(sku1, sku2));

        ProductCreateRequest.AttributeCreateRequest attr = new ProductCreateRequest.AttributeCreateRequest();
        attr.setAttrName("品牌");
        attr.setAttrValue("Tailor");
        attr.setAttrType(1);
        attr.setSort(0);
        request.setAttributes(Collections.singletonList(attr));

        request.setTagIds(Arrays.asList(1L, 2L));

        when(distributedLock.tryLock(anyString(), anyLong(), any(TimeUnit.class))).thenReturn("token-123");
        when(productMapper.selectCount(any())).thenReturn(0L);
        when(productMapper.insert(any(Product.class))).thenAnswer(invocation -> {
            Product p = invocation.getArgument(0);
            p.setId(1L);
            return 1;
        });

        Long productId = productService.createProduct(request);

        assertNotNull(productId);
        assertEquals(1L, productId);
        verify(productMapper).insert(any(Product.class));
        verify(productSkuMapper).insertBatchSomeColumn(any());
        verify(productAttributeMapper).insertBatchSomeColumn(any());
        verify(productTagMappingMapper).insertBatchSomeColumn(any());
    }

    @Test
    @DisplayName("获取产品详情缓存命中")
    void testGetProductDetail_CacheHit() throws Exception {
        Long productId = 1L;
        Product cachedProduct = buildProduct(productId);

        String cacheJson = objectMapper.writeValueAsString(cachedProduct);

        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(RedisKeyPrefix.PRODUCT + "detail:" + productId)).thenReturn(cacheJson);

        Product result = productService.getProductDetail(productId);

        assertNotNull(result);
        assertEquals(productId, result.getId());
        assertEquals("测试商品", result.getName());
        verify(productMapper, never()).selectById(anyLong());
    }

    @Test
    @DisplayName("获取产品详情缓存未命中查数据库")
    void testGetProductDetail_CacheMiss() {
        Long productId = 1L;
        Product dbProduct = buildProduct(productId);

        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(RedisKeyPrefix.PRODUCT + "detail:" + productId)).thenReturn(null);
        when(productMapper.selectById(productId)).thenReturn(dbProduct);
        when(productMapper.updateById(any(Product.class))).thenReturn(1);
        when(distributedLock.executeWithLock(anyString(), anyLong(), anyLong(), any(TimeUnit.class), any()))
                .thenAnswer(invocation -> {
                    java.util.function.Supplier<?> task = invocation.getArgument(4);
                    return task.get();
                });

        Product result = productService.getProductDetail(productId);

        assertNotNull(result);
        assertEquals(productId, result.getId());
        verify(productMapper).selectById(productId);
        verify(stringRedisTemplate.opsForValue(), atLeastOnce()).get(anyString());
    }

    @Test
    @DisplayName("获取产品详情商品不存在抛异常")
    void testGetProductDetail_NotFound() {
        Long productId = 999L;

        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(RedisKeyPrefix.PRODUCT + "detail:" + productId)).thenReturn(null);
        when(productMapper.selectById(productId)).thenReturn(null);
        when(distributedLock.executeWithLock(anyString(), anyLong(), anyLong(), any(TimeUnit.class), any()))
                .thenAnswer(invocation -> {
                    java.util.function.Supplier<?> task = invocation.getArgument(4);
                    return task.get();
                });

        BusinessException exception = assertThrows(BusinessException.class,
                () -> productService.getProductDetail(productId));

        assertEquals("商品不存在", exception.getMessage());
    }

    @Test
    @DisplayName("更新不存在的商品抛异常")
    void testUpdateProduct_NotFound() {
        Long productId = 999L;
        ProductUpdateRequest request = buildProductUpdateRequest();

        when(productMapper.selectById(productId)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> productService.updateProduct(productId, request));

        assertEquals("商品不存在", exception.getMessage());
        verify(productMapper, never()).updateById(any(Product.class));
    }

    @Test
    @DisplayName("删除商品成功")
    void testDeleteProduct_Success() {
        Long productId = 1L;
        Product existing = buildProduct(productId);
        existing.setStatus(0);

        when(productMapper.selectById(productId)).thenReturn(existing);
        when(productMapper.deleteById(productId)).thenReturn(1);
        when(productSkuMapper.delete(any())).thenReturn(0);
        when(productAttributeMapper.delete(any())).thenReturn(0);
        when(productTagMappingMapper.delete(any())).thenReturn(0);
        when(stringRedisTemplate.delete(RedisKeyPrefix.PRODUCT + "detail:" + productId)).thenReturn(true);

        assertDoesNotThrow(() -> productService.deleteProduct(productId));

        verify(productMapper).deleteById(productId);
        verify(productSkuMapper).delete(any());
        verify(productAttributeMapper).delete(any());
        verify(productTagMappingMapper).delete(any());
        verify(stringRedisTemplate).delete(RedisKeyPrefix.PRODUCT + "detail:" + productId);
    }

    private Product buildProduct(Long id) {
        Product product = new Product();
        product.setId(id);
        product.setMerchantId(1L);
        product.setShopId(1L);
        product.setCategoryId(10L);
        product.setProductType(1);
        product.setName("测试商品");
        product.setMainImage("/images/test.jpg");
        product.setStatus(1);
        product.setAuditStatus(2);
        product.setViewCount(0);
        product.setSaleCount(0);
        product.setCommentCount(0);
        product.setFavorableRate(new BigDecimal("100.00"));
        return product;
    }

    private ProductCreateRequest buildProductCreateRequest() {
        ProductCreateRequest request = new ProductCreateRequest();
        request.setName("新商品");
        request.setCategoryId(10L);
        request.setProductType(1);
        request.setMerchantId(1L);
        request.setShopId(1L);
        request.setMainImage("/images/new.jpg");
        request.setDescription("商品描述");
        return request;
    }

    private ProductUpdateRequest buildProductUpdateRequest() {
        ProductUpdateRequest request = new ProductUpdateRequest();
        request.setName("更新商品");
        request.setCategoryId(10L);
        request.setProductType(1);
        request.setMainImage("/images/updated.jpg");
        request.setDescription("更新后的描述");
        return request;
    }
}