package com.tailoris.product.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tailoris.common.dto.PageResponse;
import com.tailoris.common.constant.RoutePortConstant;
import com.tailoris.common.result.Result;
import com.tailoris.product.dto.ProductQueryRequest;
import com.tailoris.product.entity.Product;
import com.tailoris.product.entity.ProductCategory;
import com.tailoris.product.enums.AuditStatusEnum;
import com.tailoris.product.enums.ProductStatusEnum;
import com.tailoris.product.service.ProductCategoryService;
import com.tailoris.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "商品公开接口", description = "首页商品展示相关接口，无需登录")
@RestController
@RequestMapping(RoutePortConstant.ROUTE_PRODUCTS)
@RequiredArgsConstructor
public class PublicProductController {

    private final ProductService productService;
    private final ProductCategoryService productCategoryService;

    @Operation(summary = "获取商品分类列表")
    @GetMapping("/categories")
    public Result<List<ProductCategory>> getCategories() {
        List<ProductCategory> categories = productCategoryService.listCategories();
        return Result.success(categories);
    }

    @Operation(summary = "获取热门商品")
    @GetMapping("/hot")
    public Result<List<Product>> getHotProducts() {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Product::getStatus, ProductStatusEnum.ON_SHELF.getCode())
               .eq(Product::getAuditStatus, AuditStatusEnum.APPROVED.getCode())
               .orderByDesc(Product::getSaleCount)
               .last("LIMIT 8");
        List<Product> products = productService.listProductsByWrapper(wrapper);
        return Result.success(products);
    }

    @Operation(summary = "获取新品上市")
    @GetMapping("/new")
    public Result<List<Product>> getNewProducts() {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Product::getStatus, ProductStatusEnum.ON_SHELF.getCode())
               .eq(Product::getAuditStatus, AuditStatusEnum.APPROVED.getCode())
               .orderByDesc(Product::getCreateTime)
               .last("LIMIT 8");
        List<Product> products = productService.listProductsByWrapper(wrapper);
        return Result.success(products);
    }

    @Operation(summary = "获取商品列表")
    @GetMapping
    public Result<PageResponse<Product>> listProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String sort) {
        ProductQueryRequest request = new ProductQueryRequest();
        request.setKeyword(keyword);
        request.setCategoryId(categoryId);
        request.setPageNum(pageNum);
        request.setPageSize(pageSize);
        request.setOrderByField(sort);
        
        Page<Product> page = productService.listProducts(request);
        return Result.success(PageResponse.of(page));
    }

    @Operation(summary = "搜索商品")
    @GetMapping("/search")
    public Result<PageResponse<Product>> searchProducts(
            @RequestParam String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String sort) {
        ProductQueryRequest request = new ProductQueryRequest();
        request.setKeyword(keyword);
        request.setCategoryId(categoryId);
        request.setPageNum(pageNum);
        request.setPageSize(pageSize);
        request.setOrderByField(sort);
        
        Page<Product> page = productService.listProducts(request);
        return Result.success(PageResponse.of(page));
    }

    @Operation(summary = "获取商品详情")
    @GetMapping("/{id}")
    public Result<Product> getProductDetail(@PathVariable Long id) {
        Product product = productService.getProductDetail(id);
        return Result.success(product);
    }

    @Operation(summary = "获取秒杀商品")
    @GetMapping("/seckill")
    public Result<List<Product>> getSeckillProducts() {
        return Result.success(List.of());
    }
}