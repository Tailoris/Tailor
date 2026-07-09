package com.tailoris.knowledge.service;

import com.tailoris.knowledge.entity.KnowledgeCategory;

import java.util.List;

public interface KnowledgeCategoryService {

    KnowledgeCategory createCategory(KnowledgeCategory category);

    KnowledgeCategory updateCategory(Long id, KnowledgeCategory category);

    void deleteCategory(Long id);

    KnowledgeCategory getCategoryById(Long id);

    List<KnowledgeCategory> getAllCategories();

    List<KnowledgeCategory> getTopLevelCategories();

    List<KnowledgeCategory> getChildCategories(Long parentId);
}