package com.tailoris.product.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tailoris.product.dto.ProductCreateRequest;
import com.tailoris.product.dto.ProductQueryRequest;
import com.tailoris.product.dto.ProductUpdateRequest;
import com.tailoris.product.entity.Product;
import com.tailoris.product.service.ProductService;
import com.tailoris.product.service.ProductSkuService;
import com.tailoris.product.service.ProductTagService;
import com.tailoris.common.result.Result;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("ProductController 测试")
@ExtendWith(MockitoExtension.class)
class ProductControllerTest {

    @Mock
    private ProductService productService;

    @Mock
    private ProductSkuService productSkuService;

    @Mock
    private ProductTagService productTagService;

    @InjectMocks
    private ProductController productController;

    @Test
    @DisplayName("获取商品列表")
    void testListProducts() {
        ProductQueryRequest request = new ProductQueryRequest();
        request.setPageNum(1);
        request.setPageSize(10);

        Page<Product> page = new Page<>(1, 10);
        page.setTotal(0);

        when(productService.listProducts(any(ProductQueryRequest.class))).thenReturn(page);

        Result<Page<Product>> result = productController.listProducts(request);

        assertNotNull(result);
        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
        verify(productService).listProducts(any(ProductQueryRequest.class));
    }

    @Test
    @DisplayName("获取商品详情")
    void testGetProductDetail() {
        Long productId = 1L;
        Product product = buildProduct(productId);

        when(productService.getProductDetail(productId)).thenReturn(product);

        Result<Product> result = productController.getProductDetail(productId);

        assertNotNull(result);
        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
        assertEquals(productId, result.getData().getId());
        assertEquals("测试商品", result.getData().getName());
        verify(productService).getProductDetail(productId);
    }

    @Test
    @DisplayName("创建商品成功")
    void testCreateProduct_Success() {
        Long userId = 100L;
        ProductCreateRequest request = buildProductCreateRequest();

        when(productService.createProduct(any(ProductCreateRequest.class))).thenReturn(1L);

        Result<Long> result = productController.createProduct(userId, request);

        assertNotNull(result);
        assertEquals(200, result.getCode());
        assertEquals(1L, result.getData());
        verify(productService).createProduct(any(ProductCreateRequest.class));
    }

    @Test
    @DisplayName("创建商品未登录返回失败")
    void testCreateProduct_Unauthorized() {
        ProductCreateRequest request = buildProductCreateRequest();

        Result<Long> result = productController.createProduct(null, request);

        assertNotNull(result);
        assertEquals(500, result.getCode());
        assertEquals("用户未登录", result.getMessage());
        assertNull(result.getData());
        verify(productService, never()).createProduct(any());
    }

    @Test
    @DisplayName("更新商品成功")
    void testUpdateProduct_Success() {
        Long userId = 100L;
        Long productId = 1L;
        ProductUpdateRequest request = buildProductUpdateRequest();

        doNothing().when(productService).updateProduct(eq(productId), any(ProductUpdateRequest.class));

        Result<Void> result = productController.updateProduct(userId, productId, request);

        assertNotNull(result);
        assertEquals(200, result.getCode());
        assertNull(result.getData());
        verify(productService).updateProduct(eq(productId), any(ProductUpdateRequest.class));
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