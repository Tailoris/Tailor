package com.tailoris.product.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tailoris.common.exception.BusinessException;
import com.tailoris.common.result.Result;
import com.tailoris.product.dto.ProductQueryRequest;
import com.tailoris.product.entity.Product;
import com.tailoris.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@SaCheckRole("admin")
@Tag(name = "后台商品管理")
@RestController
@RequestMapping("/api/admin/product")
@RequiredArgsConstructor
public class AdminProductController {

    private final ProductService productService;

    @Operation(summary = "查询商品列表（管理端）")
    @GetMapping("/list")
    public Result<Page<Product>> listProducts(ProductQueryRequest request) {
        Page<Product> page = productService.listProducts(request);
        return Result.success(page);
    }

    @Operation(summary = "审核商品")
    @PutMapping("/audit/{id}")
    public Result<Void> auditProduct(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @PathVariable Long id,
            @RequestParam Integer auditStatus,
            @RequestParam(required = false) String auditRemark) {
        if (userId == null) {
            return Result.fail("用户未登录");
        }
        productService.auditProduct(id, auditStatus, auditRemark, userId);
        return Result.success();
    }

    @Operation(summary = "获取商品统计信息")
    @GetMapping("/stats")
    public Result<Map<String, Object>> getProductStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("total", 0);
        stats.put("draft", 0);
        stats.put("pending", 0);
        stats.put("online", 0);
        stats.put("offline", 0);
        stats.put("rejected", 0);
        return Result.success(stats);
    }
}
