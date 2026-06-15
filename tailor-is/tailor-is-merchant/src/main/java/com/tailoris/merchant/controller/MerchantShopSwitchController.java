package com.tailoris.merchant.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.tailoris.common.result.Result;
import com.tailoris.merchant.entity.MerchantCurrentShop;
import com.tailoris.merchant.entity.MerchantShop;
import com.tailoris.merchant.service.IMerchantCurrentShopService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 商家多店铺切换控制器 - MER-003.
 *
 * @author Tailor IS Team
 * @since 2026-06-03
 */
@Tag(name = "Merchant Shop Switch API", description = "商家多店铺切换接口 - MER-003")
@RestController
@RequestMapping("/api/v1/merchant/shop-switch")
@RequiredArgsConstructor
@Validated
public class MerchantShopSwitchController {

    private final IMerchantCurrentShopService currentShopService;

    @Operation(summary = "获取当前操作店铺ID")
    @GetMapping("/current")
    @SaCheckLogin
    public Result<Long> getCurrentShopId(
            @RequestParam Long userId,
            @RequestParam Long merchantId) {
        return Result.success(currentShopService.getCurrentShopId(userId, merchantId));
    }

    @Operation(summary = "切换到指定店铺")
    @PostMapping("/switch")
    @SaCheckLogin
    public Result<Boolean> switchTo(
            @RequestParam Long userId,
            @RequestParam Long merchantId,
            @RequestParam Long targetShopId) {
        return Result.success(currentShopService.switchTo(userId, merchantId, targetShopId));
    }

    @Operation(summary = "获取用户在该商家下可见店铺列表")
    @GetMapping("/list")
    @SaCheckLogin
    public Result<List<MerchantShop>> listUserShops(
            @RequestParam Long userId,
            @RequestParam Long merchantId) {
        return Result.success(currentShopService.listUserShops(userId, merchantId));
    }

    @Operation(summary = "清除当前店铺记录")
    @PostMapping("/clear")
    @SaCheckLogin
    public Result<Boolean> clear(
            @RequestParam Long userId,
            @RequestParam Long merchantId) {
        return Result.success(currentShopService.clear(userId, merchantId));
    }
}
