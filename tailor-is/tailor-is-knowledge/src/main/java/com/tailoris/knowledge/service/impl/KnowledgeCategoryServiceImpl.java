package com.tailoris.knowledge.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tailoris.knowledge.entity.KnowledgeCategory;
import com.tailoris.knowledge.mapper.KnowledgeCategoryMapper;
import com.tailoris.knowledge.service.KnowledgeCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class KnowledgeCategoryServiceImpl extends ServiceImpl<KnowledgeCategoryMapper, KnowledgeCategory> implements KnowledgeCategoryService {

    @Override
    @Transactional
    public KnowledgeCategory createCategory(KnowledgeCategory category) {
        category.setStatus(1);
        category.setArticleCount(0);
        category.setSortOrder(category.getSortOrder() != null ? category.getSortOrder() : 0);
        save(category);
        return category;
    }

    @Override
    @Transactional
    public KnowledgeCategory updateCategory(Long id, KnowledgeCategory category) {
        KnowledgeCategory existing = getById(id);
        if (existing == null) {
            throw new RuntimeException("分类不存在");
        }
        category.setId(id);
        updateById(category);
        return getById(id);
    }

    @Override
    @Transactional
    public void deleteCategory(Long id) {
        removeById(id);
    }

    @Override
    public KnowledgeCategory getCategoryById(Long id) {
        return getById(id);
    }

    @Override
    public List<KnowledgeCategory> getAllCategories() {
        LambdaQueryWrapper<KnowledgeCategory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgeCategory::getStatus, 1);
        wrapper.orderByAsc(KnowledgeCategory::getParentId).orderByAsc(KnowledgeCategory::getSortOrder);
        return list(wrapper);
    }

    @Override
    public List<KnowledgeCategory> getTopLevelCategories() {
        LambdaQueryWrapper<KnowledgeCategory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgeCategory::getStatus, 1);
        wrapper.eq(KnowledgeCategory::getParentId, 0);
        wrapper.orderByAsc(KnowledgeCategory::getSortOrder);
        return list(wrapper);
    }

    @Override
    public List<KnowledgeCategory> getChildCategories(Long parentId) {
        LambdaQueryWrapper<KnowledgeCategory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgeCategory::getStatus, 1);
        wrapper.eq(KnowledgeCategory::getParentId, parentId);
        wrapper.orderByAsc(KnowledgeCategory::getSortOrder);
        return list(wrapper);
    }
}