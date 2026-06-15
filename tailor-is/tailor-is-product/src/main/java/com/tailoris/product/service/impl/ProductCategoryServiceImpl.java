package com.tailoris.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tailoris.common.exception.BusinessException;
import com.tailoris.product.dto.CategoryRequest;
import com.tailoris.product.entity.ProductCategory;
import com.tailoris.product.mapper.ProductCategoryMapper;
import com.tailoris.product.service.ProductCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductCategoryServiceImpl implements ProductCategoryService {

    private final ProductCategoryMapper productCategoryMapper;
    private final StringRedisTemplate stringRedisTemplate;

    private static final String CATEGORY_CACHE_KEY = "product:category:tree";
    private static final long CACHE_EXPIRE_HOURS = 24;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createCategory(CategoryRequest request) {
        if (request.getParentId() == null) {
            request.setParentId(0L);
        }
        if (request.getLevel() == null) {
            request.setLevel(1);
        }
        if (request.getSort() == null) {
            request.setSort(0);
        }
        if (request.getStatus() == null) {
            request.setStatus(1);
        }

        ProductCategory category = new ProductCategory();
        category.setName(request.getName());
        category.setParentId(request.getParentId());
        category.setLevel(request.getLevel());
        category.setSort(request.getSort());
        category.setIcon(request.getIcon());
        category.setImage(request.getImage());
        category.setStatus(request.getStatus());
        category.setDescription(request.getDescription());

        productCategoryMapper.insert(category);
        evictCache();
        return category.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCategory(Long id, CategoryRequest request) {
        ProductCategory existing = productCategoryMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("分类不存在");
        }

        ProductCategory category = new ProductCategory();
        category.setId(id);
        category.setName(request.getName());
        category.setParentId(request.getParentId());
        category.setLevel(request.getLevel());
        category.setSort(request.getSort());
        category.setIcon(request.getIcon());
        category.setImage(request.getImage());
        category.setStatus(request.getStatus());
        category.setDescription(request.getDescription());

        productCategoryMapper.updateById(category);
        evictCache();
    }

    @Override
    public List<ProductCategory> getCategoryTree() {
        String cached = stringRedisTemplate.opsForValue().get(CATEGORY_CACHE_KEY);
        if (StringUtils.hasText(cached)) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                com.fasterxml.jackson.databind.JavaType type = mapper.getTypeFactory().constructCollectionType(List.class, ProductCategory.class);
                return mapper.readValue(cached, type);
            } catch (Exception e) {
                stringRedisTemplate.delete(CATEGORY_CACHE_KEY);
            }
        }

        LambdaQueryWrapper<ProductCategory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProductCategory::getStatus, 1)
               .orderByAsc(ProductCategory::getSort)
               .orderByAsc(ProductCategory::getId);
        List<ProductCategory> allCategories = productCategoryMapper.selectList(wrapper);

        Map<Long, List<ProductCategory>> parentMap = allCategories.stream()
                .collect(Collectors.groupingBy(ProductCategory::getParentId));

        List<ProductCategory> tree = buildTree(0L, parentMap);

        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            stringRedisTemplate.opsForValue().set(CATEGORY_CACHE_KEY, mapper.writeValueAsString(tree), CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
        } catch (Exception e) {
            // ignore cache write failure
        }

        return tree;
    }

    private List<ProductCategory> buildTree(Long parentId, Map<Long, List<ProductCategory>> parentMap) {
        List<ProductCategory> children = parentMap.get(parentId);
        if (children == null || children.isEmpty()) {
            return Collections.emptyList();
        }
        for (ProductCategory child : children) {
            child.setChildren(buildTree(child.getId(), parentMap));
        }
        return children;
    }

    @Override
    public List<ProductCategory> listCategories() {
        LambdaQueryWrapper<ProductCategory> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(ProductCategory::getSort)
               .orderByAsc(ProductCategory::getId);
        return productCategoryMapper.selectList(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCategoryStatus(Long id, Integer status) {
        ProductCategory existing = productCategoryMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("分类不存在");
        }

        ProductCategory category = new ProductCategory();
        category.setId(id);
        category.setStatus(status);
        productCategoryMapper.updateById(category);
        evictCache();
    }

    private void evictCache() {
        stringRedisTemplate.delete(CATEGORY_CACHE_KEY);
    }
}
