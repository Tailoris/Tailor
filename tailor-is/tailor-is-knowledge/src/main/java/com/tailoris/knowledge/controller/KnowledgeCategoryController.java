package com.tailoris.knowledge.controller;

import com.tailoris.common.result.Result;
import com.tailoris.knowledge.dto.CategoryRequest;
import com.tailoris.knowledge.entity.KnowledgeCategory;
import com.tailoris.knowledge.service.KnowledgeCategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/knowledge/category")
@RequiredArgsConstructor
public class KnowledgeCategoryController {

    private final KnowledgeCategoryService knowledgeCategoryService;

    @PostMapping
    public Result<KnowledgeCategory> createCategory(@Valid @RequestBody CategoryRequest request) {
        KnowledgeCategory category = new KnowledgeCategory();
        category.setName(request.getName());
        category.setParentId(request.getParentId() != null ? request.getParentId() : 0L);
        category.setDescription(request.getDescription());
        category.setIcon(request.getIcon());
        category.setSortOrder(request.getSortOrder());
        
        KnowledgeCategory created = knowledgeCategoryService.createCategory(category);
        return Result.success(created);
    }

    @PutMapping("/{id}")
    public Result<KnowledgeCategory> updateCategory(@PathVariable Long id, @Valid @RequestBody CategoryRequest request) {
        KnowledgeCategory category = new KnowledgeCategory();
        category.setName(request.getName());
        category.setParentId(request.getParentId());
        category.setDescription(request.getDescription());
        category.setIcon(request.getIcon());
        category.setSortOrder(request.getSortOrder());
        
        KnowledgeCategory updated = knowledgeCategoryService.updateCategory(id, category);
        return Result.success(updated);
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteCategory(@PathVariable Long id) {
        knowledgeCategoryService.deleteCategory(id);
        return Result.success();
    }

    @GetMapping("/{id}")
    public Result<KnowledgeCategory> getCategoryById(@PathVariable Long id) {
        KnowledgeCategory category = knowledgeCategoryService.getCategoryById(id);
        return Result.success(category);
    }

    @GetMapping("/all")
    public Result<List<KnowledgeCategory>> getAllCategories() {
        List<KnowledgeCategory> categories = knowledgeCategoryService.getAllCategories();
        return Result.success(categories);
    }

    @GetMapping("/top-level")
    public Result<List<KnowledgeCategory>> getTopLevelCategories() {
        List<KnowledgeCategory> categories = knowledgeCategoryService.getTopLevelCategories();
        return Result.success(categories);
    }

    @GetMapping("/parent/{parentId}")
    public Result<List<KnowledgeCategory>> getChildCategories(@PathVariable Long parentId) {
        List<KnowledgeCategory> categories = knowledgeCategoryService.getChildCategories(parentId);
        return Result.success(categories);
    }
}