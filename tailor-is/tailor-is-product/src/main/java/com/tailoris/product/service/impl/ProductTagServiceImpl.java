package com.tailoris.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tailoris.common.exception.BusinessException;
import com.tailoris.product.entity.ProductTag;
import com.tailoris.product.entity.ProductTagMapping;
import com.tailoris.product.mapper.ProductTagMapper;
import com.tailoris.product.mapper.ProductTagMappingMapper;
import com.tailoris.product.service.ProductTagService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductTagServiceImpl implements ProductTagService {

    private final ProductTagMapper productTagMapper;
    private final ProductTagMappingMapper productTagMappingMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createTag(ProductTag tag) {
        if (tag.getColor() == null) {
            tag.setColor("#409EFF");
        }
        if (tag.getSort() == null) {
            tag.setSort(0);
        }
        if (tag.getStatus() == null) {
            tag.setStatus(1);
        }
        productTagMapper.insert(tag);
        return tag.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateTag(Long id, ProductTag tag) {
        ProductTag existing = productTagMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("标签不存在");
        }
        tag.setId(id);
        productTagMapper.updateById(tag);
    }

    @Override
    public List<ProductTag> listTags() {
        LambdaQueryWrapper<ProductTag> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProductTag::getStatus, 1)
               .orderByAsc(ProductTag::getSort)
               .orderByAsc(ProductTag::getId);
        return productTagMapper.selectList(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignTagToProduct(Long productId, Long tagId) {
        LambdaQueryWrapper<ProductTagMapping> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProductTagMapping::getProductId, productId)
               .eq(ProductTagMapping::getTagId, tagId);
        Long count = productTagMappingMapper.selectCount(wrapper);
        if (count > 0) {
            throw new BusinessException("标签已关联");
        }
        ProductTagMapping mapping = new ProductTagMapping();
        mapping.setProductId(productId);
        mapping.setTagId(tagId);
        productTagMappingMapper.insert(mapping);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeTagFromProduct(Long productId, Long tagId) {
        LambdaQueryWrapper<ProductTagMapping> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProductTagMapping::getProductId, productId)
               .eq(ProductTagMapping::getTagId, tagId);
        productTagMappingMapper.delete(wrapper);
    }

    @Override
    public List<ProductTag> getTagsByProduct(Long productId) {
        LambdaQueryWrapper<ProductTagMapping> mappingWrapper = new LambdaQueryWrapper<>();
        mappingWrapper.eq(ProductTagMapping::getProductId, productId);
        List<ProductTagMapping> mappings = productTagMappingMapper.selectList(mappingWrapper);
        if (mappings.isEmpty()) {
            return List.of();
        }
        List<Long> tagIds = mappings.stream().map(ProductTagMapping::getTagId).collect(Collectors.toList());
        LambdaQueryWrapper<ProductTag> tagWrapper = new LambdaQueryWrapper<>();
        tagWrapper.in(ProductTag::getId, tagIds);
        return productTagMapper.selectList(tagWrapper);
    }
}
