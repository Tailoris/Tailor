package com.tailoris.product.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tailoris.common.exception.BusinessException;
import com.tailoris.common.result.Result;
import com.tailoris.product.dto.CreateReviewRequest;
import com.tailoris.product.entity.ProductReview;
import com.tailoris.product.service.ProductReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@SaCheckLogin
@Tag(name = "商品评价管理")
@RestController
@RequestMapping("/api/product/review")
@RequiredArgsConstructor
public class ReviewController {

    private final ProductReviewService productReviewService;

    @Operation(summary = "提交商品评价")
    @PostMapping
    public Result<Long> createReview(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @Valid @RequestBody CreateReviewRequest request) {
        if (userId == null) {
            return Result.fail("用户未登录");
        }
        Long id = productReviewService.createReview(userId, request);
        return Result.success(id);
    }

    @Operation(summary = "查询商品评价列表")
    @GetMapping("/{productId}")
    public Result<Page<ProductReview>> listReviews(
            @Parameter(description = "商品ID") @PathVariable Long productId,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") Integer pageSize) {
        Page<ProductReview> page = productReviewService.listReviewsByProduct(productId, pageNum, pageSize);
        return Result.success(page);
    }

    @Operation(summary = "删除评价")
    @DeleteMapping("/{id}")
    public Result<Void> deleteReview(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @PathVariable Long id) {
        if (userId == null) {
            return Result.fail("用户未登录");
        }
        productReviewService.deleteReview(id);
        return Result.success();
    }

    @Operation(summary = "查询待审核评价")
    @GetMapping("/admin/pending")
    public Result<Page<ProductReview>> listPendingReviews(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") Integer pageSize) {
        Page<ProductReview> page = productReviewService.listPendingReviews(pageNum, pageSize);
        return Result.success(page);
    }

    @Operation(summary = "审核评价")
    @PutMapping("/{id}/audit")
    public Result<Void> auditReview(
            @Parameter(description = "评价ID") @PathVariable Long id,
            @Parameter(description = "审核状态") @RequestParam Integer status) {
        productReviewService.auditReview(id, status);
        return Result.success();
    }
}
