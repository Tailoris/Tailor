package com.tailoris.knowledge.controller;

import com.tailoris.common.result.Result;
import com.tailoris.common.constant.RoutePortConstant;
import com.tailoris.knowledge.dto.CategoryRequest;
import com.tailoris.knowledge.entity.KnowledgeCategory;
import com.tailoris.knowledge.service.KnowledgeCategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(RoutePortConstant.ROUTE_KNOWLEDGE + "/category")
@RequiredArgsConstructor
public class KnowledgeCategoryController {

    private final KnowledgeCategoryService knowledgeCategoryService;

    @Operation(summary = "创建分类", description = "创建新的知识分类")
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

    @Operation(summary = "更新分类", description = "更新指定知识分类")
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

    @Operation(summary = "删除分类", description = "删除指定知识分类")
    @DeleteMapping("/{id}")
    public Result<Void> deleteCategory(@PathVariable Long id) {
        knowledgeCategoryService.deleteCategory(id);
        return Result.success();
    }

    @Operation(summary = "分类详情", description = "获取知识分类详情")
    @GetMapping("/{id}")
    public Result<KnowledgeCategory> getCategoryById(@PathVariable Long id) {
        KnowledgeCategory category = knowledgeCategoryService.getCategoryById(id);
        return Result.success(category);
    }

    @Operation(summary = "全部分类", description = "获取所有知识分类")
    @GetMapping("/all")
    public Result<List<KnowledgeCategory>> getAllCategories() {
        List<KnowledgeCategory> categories = knowledgeCategoryService.getAllCategories();
        return Result.success(categories);
    }

    @Operation(summary = "顶级分类", description = "获取顶级知识分类列表")
    @GetMapping("/top-level")
    public Result<List<KnowledgeCategory>> getTopLevelCategories() {
        List<KnowledgeCategory> categories = knowledgeCategoryService.getTopLevelCategories();
        return Result.success(categories);
    }

    @Operation(summary = "子分类列表", description = "获取指定父分类的子分类列表")
    @GetMapping("/parent/{parentId}")
    public Result<List<KnowledgeCategory>> getChildCategories(@PathVariable Long parentId) {
        List<KnowledgeCategory> categories = knowledgeCategoryService.getChildCategories(parentId);
        return Result.success(categories);
    }
}