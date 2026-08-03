package com.tailoris.admin.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaCheckRole;
import com.tailoris.api.admin.dto.ShopCategoryDTO;
import com.tailoris.api.admin.dto.ShopCategoryQueryRequest;
import com.tailoris.common.constant.RoutePortConstant;
import com.tailoris.admin.service.AdminShopCategoryService;
import com.tailoris.common.dto.PageResponse;
import com.tailoris.common.result.Result;
import com.tailoris.api.merchant.entity.ShopCategory;
import com.tailoris.common.util.AdminSecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@SaCheckRole({"SUPER_ADMIN", "PLATFORM_ADMIN"})
@Tag(name = "平台商家管理-店铺分类")
@RestController
@RequestMapping(RoutePortConstant.ROUTE_ADMIN_SHOP_CATEGORY)
@RequiredArgsConstructor
public class AdminShopCategoryController {

    private final AdminShopCategoryService adminShopCategoryService;

    @SaCheckPermission("merchant:category:view")
    @Operation(summary = "获取店铺分类树（2级树，root为0）")
    @GetMapping("/tree")
    public Result<List<ShopCategory>> tree() {
        return Result.success(adminShopCategoryService.tree());
    }

    @SaCheckPermission("merchant:category:view")
    @Operation(summary = "分页查询店铺分类")
    @GetMapping("/page")
    public Result<PageResponse<ShopCategory>> page(ShopCategoryQueryRequest req) {
        return Result.success(adminShopCategoryService.page(req));
    }

    @SaCheckPermission("merchant:category:view")
    @Operation(summary = "获取店铺分类详情")
    @GetMapping("/{id}")
    public Result<ShopCategory> getById(@PathVariable Long id) {
        return Result.success(adminShopCategoryService.getById(id));
    }

    @SaCheckPermission("merchant:category:add")
    @Operation(summary = "新增店铺分类")
    @PostMapping
    public Result<Void> create(@Valid @RequestBody ShopCategoryDTO dto) {
        adminShopCategoryService.create(dto, AdminSecurityUtils.getLoginIdAsLong());
        return Result.success();
    }

    @SaCheckPermission("merchant:category:edit")
    @Operation(summary = "修改店铺分类")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody ShopCategoryDTO dto) {
        adminShopCategoryService.update(id, dto, AdminSecurityUtils.getLoginIdAsLong());
        return Result.success();
    }

    @SaCheckPermission("merchant:category:delete")
    @Operation(summary = "删除店铺分类")
    @DeleteMapping("/{id}")
    public Result<Void> remove(@PathVariable Long id) {
        adminShopCategoryService.remove(id, AdminSecurityUtils.getLoginIdAsLong());
        return Result.success();
    }
}
