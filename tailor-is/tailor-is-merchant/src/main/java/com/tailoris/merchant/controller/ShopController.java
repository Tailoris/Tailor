package com.tailoris.merchant.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.tailoris.common.result.Result;
import com.tailoris.merchant.dto.ShopDecorationRequest;
import com.tailoris.merchant.dto.ShopUpdateRequest;
import com.tailoris.merchant.entity.MerchantShop;
import com.tailoris.merchant.service.MerchantShopService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@SaCheckLogin
@Tag(name = "店铺管理")
@RestController
@RequestMapping("/api/merchant/shop")
@RequiredArgsConstructor
public class ShopController {

    private final MerchantShopService merchantShopService;

    @Operation(summary = "创建店铺")
    @PostMapping
    public Result<MerchantShop> createShop(
            @RequestHeader(value = "X-Merchant-Id", required = false) Long merchantId,
            @Valid @RequestBody ShopUpdateRequest request) {
        if (merchantId == null) {
            return Result.fail("商家未登录");
        }
        MerchantShop shop = merchantShopService.createShop(merchantId, request);
        return Result.success(shop);
    }

    @Operation(summary = "更新店铺")
    @PutMapping("/{id}")
    public Result<Void> updateShop(
            @RequestHeader(value = "X-Merchant-Id", required = false) Long merchantId,
            @PathVariable Long id,
            @Valid @RequestBody ShopUpdateRequest request) {
        if (merchantId == null) {
            return Result.fail("商家未登录");
        }
        merchantShopService.updateShop(merchantId, id, request);
        return Result.success();
    }

    @Operation(summary = "获取店铺信息")
    @GetMapping("/{id}")
    public Result<MerchantShop> getShopInfo(@PathVariable Long id) {
        MerchantShop shop = merchantShopService.getShopInfo(id);
        if (shop == null) {
            return Result.fail("店铺不存在");
        }
        return Result.success(shop);
    }

    @Operation(summary = "查询商家店铺列表")
    @GetMapping("/list/{merchantId}")
    public Result<List<MerchantShop>> listShopsByMerchant(@PathVariable Long merchantId) {
        List<MerchantShop> shops = merchantShopService.listShopsByMerchant(merchantId);
        return Result.success(shops);
    }

    @Operation(summary = "更新店铺状态")
    @PutMapping("/{id}/status")
    public Result<Void> updateShopStatus(
            @RequestHeader(value = "X-Merchant-Id", required = false) Long merchantId,
            @PathVariable Long id,
            @RequestParam Integer shopStatus) {
        if (merchantId == null) {
            return Result.fail("商家未登录");
        }
        merchantShopService.updateShopStatus(merchantId, id, shopStatus);
        return Result.success();
    }

    @Operation(summary = "保存店铺装修配置 - MER-004")
    @PostMapping("/decoration")
    public Result<MerchantShop> saveDecoration(
            @RequestHeader(value = "X-Merchant-Id", required = false) Long merchantId,
            @Valid @RequestBody ShopDecorationRequest request) {
        if (merchantId == null) {
            return Result.fail("商家未登录");
        }
        return Result.success(merchantShopService.saveDecoration(merchantId, request));
    }

    @Operation(summary = "获取店铺装修配置 - MER-004")
    @GetMapping("/decoration/{shopId}")
    public Result<Map<String, Object>> getDecoration(@PathVariable Long shopId) {
        return Result.success(merchantShopService.getDecoration(shopId));
    }

    @Operation(summary = "预览装修效果 - MER-004")
    @GetMapping("/decoration/{shopId}/preview")
    public Result<Map<String, Object>> previewDecoration(@PathVariable Long shopId) {
        return Result.success(merchantShopService.previewDecoration(shopId));
    }
}
