package com.tailoris.product.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tailoris.common.constant.RoutePortConstant;
import com.tailoris.common.result.Result;
import com.tailoris.product.entity.ProductFavorite;
import com.tailoris.product.service.ProductFavoriteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 商品收藏 Controller - USR-008.
 *
 * @author Tailor IS Team
 * @since 1.0.0
 */
@Tag(name = "商品收藏", description = "用户收藏商品相关接口")
@RestController
@RequestMapping(RoutePortConstant.ROUTE_FAVORITE)
@RequiredArgsConstructor
public class ProductFavoriteController {

    private final ProductFavoriteService productFavoriteService;

    @Operation(summary = "添加收藏")
    @PostMapping("/{productId}")
    public Result<Void> add(
            @RequestHeader("X-User-Id") Long userId,
            @Parameter(description = "商品ID") @PathVariable Long productId) {
        productFavoriteService.addFavorite(userId, productId);
        return Result.success();
    }

    @Operation(summary = "取消收藏")
    @PostMapping("/cancel/{productId}")
    public Result<Void> cancel(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long productId) {
        productFavoriteService.removeFavorite(userId, productId);
        return Result.success();
    }

    @Operation(summary = "判断是否已收藏")
    @GetMapping("/check/{productId}")
    public Result<Boolean> check(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long productId) {
        boolean favorited = productFavoriteService.isFavorited(userId, productId);
        return Result.success(favorited);
    }

    @Operation(summary = "我的收藏列表")
    @GetMapping("/list")
    public Result<Page<ProductFavorite>> list(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        return Result.success(productFavoriteService.listFavorites(userId, pageNum, pageSize));
    }

    @Operation(summary = "我的收藏列表（根路径）")
    @GetMapping
    public Result<Page<ProductFavorite>> listRoot(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        return Result.success(productFavoriteService.listFavorites(userId, pageNum, pageSize));
    }
}
