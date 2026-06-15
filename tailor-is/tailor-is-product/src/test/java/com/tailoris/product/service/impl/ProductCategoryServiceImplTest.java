package com.tailoris.product.service.impl;

import com.tailoris.common.exception.BusinessException;
import com.tailoris.product.dto.CategoryRequest;
import com.tailoris.product.entity.ProductCategory;
import com.tailoris.product.mapper.ProductCategoryMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("ProductCategoryServiceImpl 测试")
@ExtendWith(MockitoExtension.class)
class ProductCategoryServiceImplTest {

    @Mock
    private ProductCategoryMapper productCategoryMapper;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private ProductCategoryServiceImpl productCategoryService;

    @Test
    @DisplayName("获取所有分类列表")
    void testListCategories() {
        List<ProductCategory> categories = Arrays.asList(
                buildCategory(1L, "服装", 0L, 1, 0),
                buildCategory(2L, "上衣", 1L, 2, 0)
        );

        when(productCategoryMapper.selectList(any())).thenReturn(categories);

        List<ProductCategory> result = productCategoryService.listCategories();

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("服装", result.get(0).getName());
        assertEquals("上衣", result.get(1).getName());
        verify(productCategoryMapper).selectList(any());
    }

    @Test
    @DisplayName("获取分类树缓存命中")
    void testGetCategoryTree_CacheHit() throws Exception {
        List<ProductCategory> cachedTree = Arrays.asList(
                buildCategoryWithChildren(1L, "服装", 0L, 1,
                        buildCategory(2L, "上衣", 1L, 2, 0))
        );

        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        String cacheJson = mapper.writeValueAsString(cachedTree);

        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("product:category:tree")).thenReturn(cacheJson);

        List<ProductCategory> result = productCategoryService.getCategoryTree();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("服装", result.get(0).getName());
        assertEquals(1, result.get(0).getChildren().size());
        assertEquals("上衣", result.get(0).getChildren().get(0).getName());
        verify(productCategoryMapper, never()).selectList(any());
    }

    @Test
    @DisplayName("获取分类树缓存未命中查数据库")
    void testGetCategoryTree_CacheMiss() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("product:category:tree")).thenReturn(null);

        List<ProductCategory> dbCategories = Arrays.asList(
                buildCategory(1L, "服装", 0L, 1, 0),
                buildCategory(2L, "上衣", 1L, 2, 0)
        );
        when(productCategoryMapper.selectList(any())).thenReturn(dbCategories);

        List<ProductCategory> result = productCategoryService.getCategoryTree();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("服装", result.get(0).getName());
        verify(productCategoryMapper).selectList(any());
    }

    @Test
    @DisplayName("创建分类成功")
    void testCreateCategory() {
        CategoryRequest request = buildCategoryRequest("新分类", 0L, 1, 0);

        when(productCategoryMapper.insert(any(ProductCategory.class))).thenAnswer(invocation -> {
            ProductCategory c = invocation.getArgument(0);
            c.setId(10L);
            return 1;
        });

        Long id = productCategoryService.createCategory(request);

        assertNotNull(id);
        assertEquals(10L, id);
        verify(productCategoryMapper).insert(any(ProductCategory.class));
        verify(stringRedisTemplate).delete("product:category:tree");
    }

    @Test
    @DisplayName("更新分类成功")
    void testUpdateCategory_Success() {
        Long categoryId = 1L;
        CategoryRequest request = buildCategoryRequest("更新分类", 0L, 1, 1);

        ProductCategory existing = buildCategory(categoryId, "旧分类", 0L, 1, 0);
        when(productCategoryMapper.selectById(categoryId)).thenReturn(existing);
        when(productCategoryMapper.updateById(any(ProductCategory.class))).thenReturn(1);

        assertDoesNotThrow(() -> productCategoryService.updateCategory(categoryId, request));

        verify(productCategoryMapper).selectById(categoryId);
        verify(productCategoryMapper).updateById(any(ProductCategory.class));
        verify(stringRedisTemplate).delete("product:category:tree");
    }

    @Test
    @DisplayName("更新不存在的分类抛异常")
    void testUpdateCategory_NotFound() {
        Long categoryId = 999L;
        CategoryRequest request = buildCategoryRequest("不存在的分类", 0L, 1, 0);

        when(productCategoryMapper.selectById(categoryId)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> productCategoryService.updateCategory(categoryId, request));

        assertEquals("分类不存在", exception.getMessage());
        verify(productCategoryMapper, never()).updateById(any(ProductCategory.class));
    }

    private ProductCategory buildCategory(Long id, String name, Long parentId, Integer level, Integer sort) {
        ProductCategory category = new ProductCategory();
        category.setId(id);
        category.setName(name);
        category.setParentId(parentId);
        category.setLevel(level);
        category.setSort(sort);
        category.setStatus(1);
        return category;
    }

    private ProductCategory buildCategoryWithChildren(Long id, String name, Long parentId, Integer level,
                                                       ProductCategory... children) {
        ProductCategory category = buildCategory(id, name, parentId, level, 0);
        category.setChildren(Arrays.asList(children));
        return category;
    }

    private CategoryRequest buildCategoryRequest(String name, Long parentId, Integer level, Integer sort) {
        CategoryRequest request = new CategoryRequest();
        request.setName(name);
        request.setParentId(parentId);
        request.setLevel(level);
        request.setSort(sort);
        request.setStatus(1);
        return request;
    }
}