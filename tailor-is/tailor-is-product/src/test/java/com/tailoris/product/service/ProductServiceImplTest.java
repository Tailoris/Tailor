package com.tailoris.product.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.tailoris.common.exception.BusinessException;
import com.tailoris.common.lock.DistributedLock;
import com.tailoris.product.dto.ProductCreateRequest;
import com.tailoris.product.entity.Product;
import com.tailoris.product.mapper.ProductAttributeMapper;
import com.tailoris.product.mapper.ProductFavoriteMapper;
import com.tailoris.product.mapper.ProductMapper;
import com.tailoris.product.mapper.ProductReviewMapper;
import com.tailoris.product.mapper.ProductSkuMapper;
import com.tailoris.product.mapper.ProductTagMappingMapper;
import com.tailoris.product.service.impl.ProductServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ProductService 单元测试 - 验证 B-C08 修复
 *
 * <p>Critical Fix 验证：
 * <ul>
 *   <li>B-C08: 商品创建并发控制（分布式锁+业务唯一性校验）</li>
 * </ul>
 *
 * @author Tailor IS Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ProductService 单元测试 - Critical B-C08")
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
    private DistributedLock distributedLock;

    private ProductServiceImpl productService;

    @BeforeEach
    void setUp() {
        productService = new ProductServiceImpl(
                productMapper,
                productSkuMapper,
                productAttributeMapper,
                productTagMappingMapper,
                productReviewMapper,
                productFavoriteMapper,
                stringRedisTemplate,
                new ObjectMapper(),
                distributedLock
        );
    }

    @Test
    @DisplayName("B-C08: 锁获取失败时应抛BusinessException")
    void testCreateProduct_LockFailed() {
        when(distributedLock.tryLock(anyString(), any(Long.class), any(TimeUnit.class)))
                .thenReturn(null);

        ProductCreateRequest request = createValidRequest();
        assertThatThrownBy(() -> productService.createProduct(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("商品正在创建中");

        verify(productMapper, never()).insert(any(Product.class));
    }

    @Test
    @DisplayName("B-C08: 存在同名商品时应拒绝")
    void testCreateProduct_DuplicateName() {
        when(distributedLock.tryLock(anyString(), any(Long.class), any(TimeUnit.class)))
                .thenReturn("token-123");
        when(productMapper.selectCount(any(Wrapper.class))).thenReturn(1L);

        ProductCreateRequest request = createValidRequest();
        assertThatThrownBy(() -> productService.createProduct(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("同名商品");
    }

    @Test
    @DisplayName("B-C08: SKU编码重复时应拒绝")
    void testCreateProduct_DuplicateSkuCodes() {
        when(distributedLock.tryLock(anyString(), any(Long.class), any(TimeUnit.class)))
                .thenReturn("token-123");
        when(productMapper.selectCount(any(Wrapper.class))).thenReturn(0L);

        ProductCreateRequest request = createValidRequest();
        ProductCreateRequest.SkuCreateRequest sku1 = new ProductCreateRequest.SkuCreateRequest();
        sku1.setSkuCode("SKU001");
        ProductCreateRequest.SkuCreateRequest sku2 = new ProductCreateRequest.SkuCreateRequest();
        sku2.setSkuCode("SKU001"); // 故意重复
        request.setSkus(Arrays.asList(sku1, sku2));

        assertThatThrownBy(() -> productService.createProduct(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("SKU编码");
    }

    @Test
    @DisplayName("B-C08: 正常创建商品应成功")
    void testCreateProduct_Success() {
        when(distributedLock.tryLock(anyString(), any(Long.class), any(TimeUnit.class)))
                .thenReturn("token-123");
        when(productMapper.selectCount(any(Wrapper.class))).thenReturn(0L);

        Product saved = new Product();
        saved.setId(1L);
        when(productMapper.insert(any(Product.class))).thenAnswer(invocation -> {
            Product p = invocation.getArgument(0);
            p.setId(1L);
            return 1;
        });

        ProductCreateRequest request = createValidRequest();
        Long result = productService.createProduct(request);

        assertThat(result).isEqualTo(1L);
        verify(distributedLock, times(1)).unlock(anyString(), anyString());
    }

    @Test
    @DisplayName("B-C08: 创建完成后应释放分布式锁")
    void testCreateProduct_ReleaseLock() {
        when(distributedLock.tryLock(anyString(), any(Long.class), any(TimeUnit.class)))
                .thenReturn("token-xyz");
        when(productMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(productMapper.insert(any(Product.class))).thenAnswer(invocation -> {
            Product p = invocation.getArgument(0);
            p.setId(1L);
            return 1;
        });

        ProductCreateRequest request = createValidRequest();
        try {
            productService.createProduct(request);
        } catch (Exception ignore) {
        }

        verify(distributedLock, times(1)).unlock(anyString(), eq("token-xyz"));
    }

    private ProductCreateRequest createValidRequest() {
        ProductCreateRequest request = new ProductCreateRequest();
        request.setMerchantId(1L);
        request.setShopId(1L);
        request.setCategoryId(1L);
        request.setName("测试商品");
        request.setProductType(1);
        return request;
    }

    private static <T> T eq(T value) {
        return org.mockito.ArgumentMatchers.eq(value);
    }
}
