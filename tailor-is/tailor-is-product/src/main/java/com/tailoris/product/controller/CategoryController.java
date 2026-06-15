package com.tailoris.product.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.tailoris.common.exception.BusinessException;
import com.tailoris.common.result.Result;
import com.tailoris.product.dto.CategoryRequest;
import com.tailoris.product.entity.ProductCategory;
import com.tailoris.product.service.ProductCategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@SaCheckLogin
@Tag(name = "商品分类管理")
@RestController
@RequestMapping("/api/product/category")
@RequiredArgsConstructor
public class CategoryController {

    private final ProductCategoryService productCategoryService;

    @Operation(summary = "获取分类树")
    @GetMapping("/tree")
    public Result<List<ProductCategory>> getCategoryTree() {
        List<ProductCategory> tree = productCategoryService.getCategoryTree();
        return Result.success(tree);
    }

    @Operation(summary = "获取分类列表")
    @GetMapping("/list")
    public Result<List<ProductCategory>> listCategories() {
        List<ProductCategory> categories = productCategoryService.listCategories();
        return Result.success(categories);
    }

    @Operation(summary = "创建分类")
    @PostMapping
    public Result<Long> createCategory(@Valid @RequestBody CategoryRequest request) {
        Long id = productCategoryService.createCategory(request);
        return Result.success(id);
    }

    @Operation(summary = "更新分类")
    @PutMapping("/{id}")
    public Result<Void> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody CategoryRequest request) {
        productCategoryService.updateCategory(id, request);
        return Result.success();
    }

    @Operation(summary = "更新分类状态")
    @PutMapping("/{id}/status")
    public Result<Void> updateCategoryStatus(
            @PathVariable Long id,
            @RequestParam Integer status) {
        productCategoryService.updateCategoryStatus(id, status);
        return Result.success();
    }
}
