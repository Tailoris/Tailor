package com.tailoris.admin.controller;

import com.tailoris.api.admin.dto.ProductAuditRequest;
import com.tailoris.admin.service.AdminProductService;
import com.tailoris.common.dto.PageResponse;
import com.tailoris.common.dto.PageRequest;
import com.tailoris.common.result.Result;
import com.tailoris.api.product.entity.Product;
import cn.dev33.satoken.annotation.SaCheckRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "平台商品管理")
@RestController
@RequestMapping("/api/admin/product")
@RequiredArgsConstructor
@SaCheckRole("admin")
public class AdminProductController {

    private final AdminProductService adminProductService;

    @Operation(summary = "查询待审核商品列表")
    @GetMapping("/pending")
    public Result<PageResponse<Product>> listPending(PageRequest request) {
        PageResponse<Product> page = adminProductService.listPendingProducts(request);
        return Result.success(page);
    }

    @Operation(summary = "商品审核")
    @PutMapping("/audit/{id}")
    public Result<Void> audit(@PathVariable Long id, @Valid @RequestBody ProductAuditRequest request) {
        request.setProductId(id);
        adminProductService.auditProduct(request, null);
        return Result.success();
    }

    @Operation(summary = "驳回商品")
    @PutMapping("/reject/{id}")
    public Result<Void> reject(@PathVariable Long id, @RequestParam String remark) {
        adminProductService.rejectProduct(id, remark, null);
        return Result.success();
    }
}
