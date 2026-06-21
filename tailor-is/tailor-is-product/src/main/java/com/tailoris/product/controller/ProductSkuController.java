package com.tailoris.product.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.tailoris.common.exception.BusinessException;
import com.tailoris.common.result.Result;
import com.tailoris.product.entity.ProductSku;
import com.tailoris.product.service.ProductSkuService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@SaCheckLogin
@Tag(name = "商品SKU管理")
@RestController
@RequestMapping("/api/product/sku")
@RequiredArgsConstructor
public class ProductSkuController {

    private final ProductSkuService productSkuService;

    @Operation(summary = "创建SKU")
    @PostMapping("/{productId}")
    public Result<Long> createSku(
            @PathVariable Long productId,
            @Valid @RequestBody ProductSku sku) {
        Long id = productSkuService.createSku(productId, sku);
        return Result.success(id);
    }

    @Operation(summary = "更新SKU")
    @PutMapping("/{id}")
    public Result<Void> updateSku(
            @PathVariable Long id,
            @Valid @RequestBody ProductSku sku) {
        productSkuService.updateSku(id, sku);
        return Result.success();
    }

    @Operation(summary = "删除SKU")
    @DeleteMapping("/{id}")
    public Result<Void> deleteSku(@PathVariable Long id) {
        productSkuService.deleteSku(id);
        return Result.success();
    }

    @Operation(summary = "查询商品SKU列表")
    @GetMapping("/product/{productId}")
    public Result<List<ProductSku>> listSkus(@PathVariable Long productId) {
        List<ProductSku> skus = productSkuService.listSkusByProduct(productId);
        return Result.success(skus);
    }

    @Operation(summary = "更新库存")
    @PutMapping("/{skuId}/stock")
    public Result<Void> updateStock(
            @Parameter(description = "SKU ID") @PathVariable Long skuId,
            @Parameter(description = "变更数量") @RequestParam Integer quantity,
            @Parameter(description = "是否增加库存") @RequestParam(defaultValue = "false") boolean increase) {
        productSkuService.updateStock(skuId, quantity, increase);
        return Result.success();
    }
}
