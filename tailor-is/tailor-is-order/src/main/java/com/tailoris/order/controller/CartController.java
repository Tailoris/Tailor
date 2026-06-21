package com.tailoris.order.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.tailoris.common.result.Result;
import com.tailoris.order.dto.CartAddRequest;
import com.tailoris.order.dto.CartUpdateRequest;
import com.tailoris.order.dto.CreateOrderRequest;
import com.tailoris.order.entity.ShoppingCart;
import com.tailoris.order.service.ShoppingCartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@SaCheckLogin
@Tag(name = "购物车管理")
@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final ShoppingCartService shoppingCartService;

    /**
     * 添加商品到购物车
     */
    @Operation(summary = "添加到购物车")
    @PostMapping
    public Result<Void> addToCart(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody CartAddRequest request) {
        shoppingCartService.addToCart(userId, request);
        return Result.success();
    }

    @Operation(summary = "更新购物车商品")
    @PutMapping("/{id}")
    public Result<Void> updateCart(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long id,
            @Valid @RequestBody CartUpdateRequest request) {
        shoppingCartService.updateCart(userId, id, request);
        return Result.success();
    }

    @Operation(summary = "删除购物车商品")
    @DeleteMapping("/{id}")
    public Result<Void> deleteCartItem(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long id) {
        shoppingCartService.deleteCartItem(userId, id);
        return Result.success();
    }

    @Operation(summary = "查询购物车列表")
    @GetMapping
    public Result<List<ShoppingCart>> listCart(
            @RequestHeader("X-User-Id") Long userId) {
        List<ShoppingCart> carts = shoppingCartService.listCart(userId);
        return Result.success(carts);
    }

    /**
     * 批量结算购物车商品
     */
    @Operation(summary = "批量结算")
    @PostMapping("/checkout")
    public Result<List<ShoppingCart>> batchCheckout(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody List<Long> cartIds) {
        List<ShoppingCart> carts = shoppingCartService.batchCheckout(userId, cartIds);
        return Result.success(carts);
    }
}
