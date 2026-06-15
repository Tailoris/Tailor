package com.tailoris.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tailoris.common.exception.BusinessException;
import com.tailoris.product.dto.CreateReviewRequest;
import com.tailoris.product.dto.ReviewReplyRequest;
import com.tailoris.product.dto.ReviewSearchRequest;
import com.tailoris.product.entity.Product;
import com.tailoris.product.entity.ProductReview;
import com.tailoris.product.mapper.ProductMapper;
import com.tailoris.product.mapper.ProductReviewMapper;
import com.tailoris.product.util.SensitiveWordFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("ProductReviewServiceImpl 商品评价服务测试")
@ExtendWith(MockitoExtension.class)
class ProductReviewServiceImplTest {

    @Mock
    private ProductReviewMapper productReviewMapper;

    @Mock
    private ProductMapper productMapper;

    @Spy
    private SensitiveWordFilter sensitiveWordFilter = new SensitiveWordFilter();

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private ProductReviewServiceImpl productReviewService;

    private Product product;
    private CreateReviewRequest createRequest;

    @BeforeEach
    void setUp() {
        product = new Product();
        product.setId(100L);
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
        product.setCommentCount(10);
        product.setFavorableRate(new BigDecimal("90.00"));

        createRequest = new CreateReviewRequest();
        createRequest.setProductId(100L);
        createRequest.setOrderId(1000L);
        createRequest.setSkuId(200L);
        createRequest.setSkuSpec("红色/XL");
        createRequest.setRating(5);
        createRequest.setContent("商品质量很好，穿着舒适");
        createRequest.setImageUrls(Arrays.asList("https://example.com/img1.jpg"));
        createRequest.setTags(Arrays.asList("质量好", "穿着舒适"));
        createRequest.setIsAnonymous(false);
    }

    @Test
    @DisplayName("创建评价成功")
    void testCreateReview_Success() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class)))
                .thenReturn(true);
        when(productMapper.selectById(100L)).thenReturn(product);
        when(productReviewMapper.insert(any(ProductReview.class))).thenAnswer(invocation -> {
            ProductReview review = invocation.getArgument(0);
            review.setId(1L);
            return 1;
        });
        when(productMapper.updateById(any(Product.class))).thenReturn(1);

        Long reviewId = productReviewService.createReview(1L, createRequest);

        assertNotNull(reviewId);
        assertEquals(1L, reviewId);
        verify(productReviewMapper).insert(any(ProductReview.class));
        verify(productMapper).updateById(any(Product.class));
    }

    @Test
    @DisplayName("创建评价失败-重复评价")
    void testCreateReview_Duplicate() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class)))
                .thenReturn(false);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> productReviewService.createReview(1L, createRequest));
        assertEquals("您已评价过该商品", exception.getMessage());
    }

    @Test
    @DisplayName("创建评价失败-商品不存在")
    void testCreateReview_ProductNotFound() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class)))
                .thenReturn(true);
        when(productMapper.selectById(100L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> productReviewService.createReview(1L, createRequest));
        assertEquals("商品不存在", exception.getMessage());
    }

    @Test
    @DisplayName("创建评价失败-图片URL非法")
    void testCreateReview_InvalidImageUrl() {
        createRequest.setImageUrls(Arrays.asList("http://invalid.com/img.jpg"));

        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class)))
                .thenReturn(true);
        when(productMapper.selectById(100L)).thenReturn(product);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> productReviewService.createReview(1L, createRequest));
        assertTrue(exception.getMessage().contains("图片URL非法"));
    }

    @Test
    @DisplayName("商家回复评价成功")
    void testMerchantReply_Success() {
        ProductReview review = new ProductReview();
        review.setId(1L);
        review.setProductId(100L);
        review.setUserId(1L);
        review.setRating(5);
        review.setContent("很好");

        ReviewReplyRequest replyRequest = new ReviewReplyRequest();
        replyRequest.setContent("感谢支持");

        when(productReviewMapper.selectById(1L)).thenReturn(review);
        when(productReviewMapper.updateById(any(ProductReview.class))).thenReturn(1);

        assertDoesNotThrow(() -> productReviewService.merchantReply(1L, 1L, replyRequest));
        assertNotNull(review.getMerchantReply());
        assertNotNull(review.getMerchantReplyTime());
        verify(productReviewMapper).updateById(review);
    }

    @Test
    @DisplayName("商家回复评价失败-评价不存在")
    void testMerchantReply_NotFound() {
        ReviewReplyRequest replyRequest = new ReviewReplyRequest();
        replyRequest.setContent("感谢支持");

        when(productReviewMapper.selectById(999L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> productReviewService.merchantReply(1L, 999L, replyRequest));
        assertEquals("评价不存在", exception.getMessage());
    }

    @Test
    @DisplayName("标记有用成功")
    void testMarkHelpful_Success() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class)))
                .thenReturn(true);
        when(productReviewMapper.incrementHelpfulCount(1L)).thenReturn(1);

        assertDoesNotThrow(() -> productReviewService.markHelpful(1L, 1L));
        verify(productReviewMapper).incrementHelpfulCount(1L);
    }

    @Test
    @DisplayName("标记有用-已标记过")
    void testMarkHelpful_AlreadyMarked() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class)))
                .thenReturn(false);

        assertDoesNotThrow(() -> productReviewService.markHelpful(1L, 1L));
        verify(productReviewMapper, never()).incrementHelpfulCount(anyLong());
    }

    @Test
    @DisplayName("举报评价成功")
    void testReportReview_Success() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class)))
                .thenReturn(true);
        when(productReviewMapper.incrementReportCount(1L)).thenReturn(1);

        assertDoesNotThrow(() -> productReviewService.reportReview(1L, 1L, "内容不当"));
        verify(productReviewMapper).incrementReportCount(1L);
    }

    @Test
    @DisplayName("举报评价失败-重复举报")
    void testReportReview_Duplicate() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class)))
                .thenReturn(false);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> productReviewService.reportReview(1L, 1L, "内容不当"));
        assertEquals("请勿重复举报", exception.getMessage());
    }

    @Test
    @DisplayName("精选评价成功")
    void testFeatureReview_Success() {
        ProductReview review = new ProductReview();
        review.setId(1L);
        review.setProductId(100L);

        when(productReviewMapper.selectById(1L)).thenReturn(review);
        when(productReviewMapper.updateById(any(ProductReview.class))).thenReturn(1);

        assertDoesNotThrow(() -> productReviewService.featureReview(1L, 1L, true));
        assertEquals(1, review.getIsFeatured());
        verify(productReviewMapper).updateById(review);
    }

    @Test
    @DisplayName("取消精选评价")
    void testUnfeatureReview() {
        ProductReview review = new ProductReview();
        review.setId(1L);
        review.setProductId(100L);
        review.setIsFeatured(1);

        when(productReviewMapper.selectById(1L)).thenReturn(review);
        when(productReviewMapper.updateById(any(ProductReview.class))).thenReturn(1);

        assertDoesNotThrow(() -> productReviewService.featureReview(1L, 1L, false));
        assertEquals(0, review.getIsFeatured());
        verify(productReviewMapper).updateById(review);
    }

    @Test
    @DisplayName("精选评价失败-评价不存在")
    void testFeatureReview_NotFound() {
        when(productReviewMapper.selectById(999L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> productReviewService.featureReview(1L, 999L, true));
        assertEquals("评价不存在", exception.getMessage());
    }

    @Test
    @DisplayName("查询商品评价列表")
    void testListProductReviews() {
        ReviewSearchRequest request = new ReviewSearchRequest();
        request.setProductId(100L);
        request.setPageNum(1);
        request.setPageSize(20);
        request.setSortBy("new");

        Page<ProductReview> page = new Page<>(1, 20);
        page.setRecords(Arrays.asList(new ProductReview()));

        when(productReviewMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                .thenReturn(page);

        Page<ProductReview> result = productReviewService.listProductReviews(request);

        assertNotNull(result);
        assertEquals(1, result.getRecords().size());
        verify(productReviewMapper).selectPage(any(Page.class), any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("查询商品评价列表-按热度排序")
    void testListProductReviews_SortByHot() {
        ReviewSearchRequest request = new ReviewSearchRequest();
        request.setProductId(100L);
        request.setPageNum(1);
        request.setPageSize(20);
        request.setSortBy("hot");

        Page<ProductReview> page = new Page<>(1, 20);
        when(productReviewMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                .thenReturn(page);

        assertDoesNotThrow(() -> productReviewService.listProductReviews(request));
        verify(productReviewMapper).selectPage(any(Page.class), any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("查询用户评价列表")
    void testListUserReviews() {
        Page<ProductReview> page = new Page<>(1, 20);
        when(productReviewMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                .thenReturn(page);

        Page<ProductReview> result = productReviewService.listUserReviews(1L, 1, 20);

        assertNotNull(result);
        verify(productReviewMapper).selectPage(any(Page.class), any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("删除评价成功")
    void testDeleteReview_Success() {
        ProductReview review = new ProductReview();
        review.setId(1L);

        when(productReviewMapper.selectById(1L)).thenReturn(review);
        when(productReviewMapper.deleteById(1L)).thenReturn(1);

        assertDoesNotThrow(() -> productReviewService.deleteReview(1L));
        verify(productReviewMapper).deleteById(1L);
    }

    @Test
    @DisplayName("删除评价失败-评价不存在")
    void testDeleteReview_NotFound() {
        when(productReviewMapper.selectById(999L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> productReviewService.deleteReview(999L));
        assertEquals("评价不存在", exception.getMessage());
    }

    @Test
    @DisplayName("审核评价成功")
    void testAuditReview_Success() {
        ProductReview review = new ProductReview();
        review.setId(1L);
        review.setStatus(0);

        when(productReviewMapper.selectById(1L)).thenReturn(review);
        when(productReviewMapper.updateById(any(ProductReview.class))).thenReturn(1);

        assertDoesNotThrow(() -> productReviewService.auditReview(1L, 1));
        assertEquals(1, review.getStatus());
        verify(productReviewMapper).updateById(review);
    }

    @Test
    @DisplayName("审核评价失败-评价不存在")
    void testAuditReview_NotFound() {
        when(productReviewMapper.selectById(999L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> productReviewService.auditReview(999L, 1));
        assertEquals("评价不存在", exception.getMessage());
    }
}
