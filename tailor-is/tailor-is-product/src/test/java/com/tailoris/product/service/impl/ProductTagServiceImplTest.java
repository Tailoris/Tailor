package com.tailoris.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tailoris.common.exception.BusinessException;
import com.tailoris.product.entity.ProductTag;
import com.tailoris.product.entity.ProductTagMapping;
import com.tailoris.product.mapper.ProductTagMapper;
import com.tailoris.product.mapper.ProductTagMappingMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("ProductTagServiceImpl 商品标签服务测试")
@ExtendWith(MockitoExtension.class)
class ProductTagServiceImplTest {

    @Mock
    private ProductTagMapper productTagMapper;

    @Mock
    private ProductTagMappingMapper productTagMappingMapper;

    @InjectMocks
    private ProductTagServiceImpl productTagService;

    private ProductTag tag;

    @BeforeEach
    void setUp() {
        tag = new ProductTag();
        tag.setId(1L);
        tag.setName("新品");
        tag.setColor("#FF0000");
        tag.setSort(1);
        tag.setStatus(1);
    }

    @Test
    @DisplayName("创建标签成功")
    void testCreateTag_Success() {
        when(productTagMapper.insert(any(ProductTag.class))).thenReturn(1);

        Long tagId = productTagService.createTag(tag);

        assertNotNull(tagId);
        assertEquals(1L, tagId);
        verify(productTagMapper).insert(tag);
    }

    @Test
    @DisplayName("创建标签-默认颜色")
    void testCreateTag_DefaultColor() {
        tag.setColor(null);
        when(productTagMapper.insert(any(ProductTag.class))).thenReturn(1);

        productTagService.createTag(tag);

        assertEquals("#409EFF", tag.getColor());
        verify(productTagMapper).insert(tag);
    }

    @Test
    @DisplayName("创建标签-默认排序")
    void testCreateTag_DefaultSort() {
        tag.setSort(null);
        when(productTagMapper.insert(any(ProductTag.class))).thenReturn(1);

        productTagService.createTag(tag);

        assertEquals(0, tag.getSort());
        verify(productTagMapper).insert(tag);
    }

    @Test
    @DisplayName("创建标签-默认状态")
    void testCreateTag_DefaultStatus() {
        tag.setStatus(null);
        when(productTagMapper.insert(any(ProductTag.class))).thenReturn(1);

        productTagService.createTag(tag);

        assertEquals(1, tag.getStatus());
        verify(productTagMapper).insert(tag);
    }

    @Test
    @DisplayName("更新标签成功")
    void testUpdateTag_Success() {
        ProductTag updateTag = new ProductTag();
        updateTag.setName("热销");

        when(productTagMapper.selectById(1L)).thenReturn(tag);
        when(productTagMapper.updateById(any(ProductTag.class))).thenReturn(1);

        assertDoesNotThrow(() -> productTagService.updateTag(1L, updateTag));
        assertEquals(1L, updateTag.getId());
        verify(productTagMapper).updateById(updateTag);
    }

    @Test
    @DisplayName("更新标签-标签不存在")
    void testUpdateTag_NotFound() {
        ProductTag updateTag = new ProductTag();
        when(productTagMapper.selectById(999L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> productTagService.updateTag(999L, updateTag));
        assertEquals("标签不存在", exception.getMessage());
    }

    @Test
    @DisplayName("查询标签列表")
    void testListTags() {
        List<ProductTag> tags = Arrays.asList(tag);
        when(productTagMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(tags);

        List<ProductTag> result = productTagService.listTags();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("新品", result.get(0).getName());
        verify(productTagMapper).selectList(any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("为商品分配标签成功")
    void testAssignTagToProduct_Success() {
        when(productTagMappingMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(productTagMappingMapper.insert(any(ProductTagMapping.class))).thenReturn(1);

        assertDoesNotThrow(() -> productTagService.assignTagToProduct(100L, 1L));
        verify(productTagMappingMapper).insert(any(ProductTagMapping.class));
    }

    @Test
    @DisplayName("为商品分配标签-已关联")
    void testAssignTagToProduct_AlreadyAssigned() {
        when(productTagMappingMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> productTagService.assignTagToProduct(100L, 1L));
        assertEquals("标签已关联", exception.getMessage());
    }

    @Test
    @DisplayName("移除商品标签")
    void testRemoveTagFromProduct() {
        when(productTagMappingMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(1);

        assertDoesNotThrow(() -> productTagService.removeTagFromProduct(100L, 1L));
        verify(productTagMappingMapper).delete(any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("获取商品标签-有关联")
    void testGetTagsByProduct_WithMappings() {
        ProductTagMapping mapping = new ProductTagMapping();
        mapping.setProductId(100L);
        mapping.setTagId(1L);

        when(productTagMappingMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.singletonList(mapping));
        when(productTagMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.singletonList(tag));

        List<ProductTag> result = productTagService.getTagsByProduct(100L);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("新品", result.get(0).getName());
    }

    @Test
    @DisplayName("获取商品标签-无关联")
    void testGetTagsByProduct_NoMappings() {
        when(productTagMappingMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.emptyList());

        List<ProductTag> result = productTagService.getTagsByProduct(100L);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}
