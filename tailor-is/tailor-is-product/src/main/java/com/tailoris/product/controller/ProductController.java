package com.tailoris.product.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tailoris.common.result.Result;
import com.tailoris.product.dto.ProductCreateRequest;
import com.tailoris.product.dto.ProductQueryRequest;
import com.tailoris.product.dto.ProductUpdateRequest;
import com.tailoris.product.entity.Product;
import com.tailoris.product.entity.ProductSku;
import com.tailoris.product.entity.ProductTag;
import com.tailoris.product.service.ProductService;
import com.tailoris.product.service.ProductSkuService;
import com.tailoris.product.service.ProductTagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Deprecated
@Tag(name = "商品管理", description = "已弃用，请使用 /api/v1/product")
@RestController
@RequestMapping("/api/product")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final ProductSkuService productSkuService;
    private final ProductTagService productTagService;

    @Operation(summary = "创建商品")
    @PostMapping
    public Result<Long> createProduct(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @Valid @RequestBody ProductCreateRequest request) {
        if (userId == null) {
            return Result.fail("用户未登录");
        }
        Long id = productService.createProduct(request);
        return Result.success(id);
    }

    @Operation(summary = "更新商品")
    @PutMapping("/{id}")
    public Result<Void> updateProduct(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @PathVariable Long id,
            @Valid @RequestBody ProductUpdateRequest request) {
        if (userId == null) {
            return Result.fail("用户未登录");
        }
        productService.updateProduct(id, request);
        return Result.success();
    }

    @Operation(summary = "删除商品")
    @DeleteMapping("/{id}")
    public Result<Void> deleteProduct(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @PathVariable Long id) {
        if (userId == null) {
            return Result.fail("用户未登录");
        }
        productService.deleteProduct(id);
        return Result.success();
    }

    @Operation(summary = "获取商品详情")
    @GetMapping("/{id}")
    public Result<Product> getProductDetail(@PathVariable Long id) {
        Product product = productService.getProductDetail(id);
        return Result.success(product);
    }

    @Operation(summary = "查询商品列表")
    @GetMapping("/list")
    public Result<Page<Product>> listProducts(ProductQueryRequest request) {
        Page<Product> page = productService.listProducts(request);
        return Result.success(page);
    }

    @Operation(summary = "查询店铺商品列表")
    @GetMapping("/shop/{shopId}")
    public Result<Page<Product>> listProductsByShop(
            @PathVariable Long shopId,
            ProductQueryRequest request) {
        Page<Product> page = productService.listProductsByShop(shopId, request);
        return Result.success(page);
    }

    @Operation(summary = "更新商品状态")
    @PutMapping("/{id}/status")
    public Result<Void> updateProductStatus(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @Parameter(description = "商品ID") @PathVariable Long id,
            @Parameter(description = "商品状态") @RequestParam Integer status) {
        if (userId == null) {
            return Result.fail("用户未登录");
        }
        productService.updateProductStatus(id, status);
        return Result.success();
    }

    @Operation(summary = "根据商品类型查询")
    @GetMapping("/type/{productType}")
    public Result<Page<Product>> getProductByType(
            @PathVariable Integer productType,
            ProductQueryRequest request) {
        Page<Product> page = productService.getProductByType(productType, request);
        return Result.success(page);
    }

    @Operation(summary = "获取商品SKU列表")
    @GetMapping("/{id}/skus")
    public Result<List<ProductSku>> listSkus(@PathVariable Long id) {
        List<ProductSku> skus = productSkuService.listSkusByProduct(id);
        return Result.success(skus);
    }

    @Operation(summary = "获取商品标签")
    @GetMapping("/{id}/tags")
    public Result<List<ProductTag>> getTags(@PathVariable Long id) {
        List<ProductTag> tags = productTagService.getTagsByProduct(id);
        return Result.success(tags);
    }
}
