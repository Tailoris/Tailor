package com.tailoris.admin.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.tailoris.admin.dto.CampaignStatusUpdateRequest;
import com.tailoris.common.constant.RoutePortConstant;
import com.tailoris.admin.dto.CouponCreateRequest;
import com.tailoris.admin.dto.CouponQueryRequest;
import com.tailoris.admin.service.AdminMarketingService;
import com.tailoris.common.dto.PageResponse;
import com.tailoris.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@SaCheckRole({"SUPER_ADMIN", "PLATFORM_ADMIN", "MARKETING_ADMIN"})
@Tag(name = "营销管理")
@RestController
@RequestMapping(RoutePortConstant.ROUTE_ADMIN_MARKETING)
@RequiredArgsConstructor
public class AdminMarketingController {

    private final AdminMarketingService adminMarketingService;

    @Operation(summary = "查询优惠券列表")
    @GetMapping("/coupons")
    public Result<PageResponse<Object>> listCoupons(CouponQueryRequest request) {
        PageResponse<Object> page = adminMarketingService.listCoupons(request);
        return Result.success(page);
    }

    @Operation(summary = "创建优惠券")
    @PostMapping("/coupons")
    public Result<Long> createCoupon(@RequestBody CouponCreateRequest request) {
        Long id = adminMarketingService.createCoupon(request);
        return Result.success(id);
    }

    @Operation(summary = "禁用优惠券")
    @PutMapping("/coupons/{id}/disable")
    public Result<Void> disableCoupon(@Parameter(description = "优惠券ID") @PathVariable Long id) {
        adminMarketingService.disableCoupon(id);
        return Result.success();
    }

    @Operation(summary = "查询活动列表")
    @GetMapping("/campaigns")
    public Result<PageResponse<Object>> listCampaigns(
            @Parameter(description = "活动状态") @RequestParam(required = false) Integer status,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") Integer pageSize) {
        PageResponse<Object> page = adminMarketingService.listCampaigns(status, pageNum, pageSize);
        return Result.success(page);
    }

    @Operation(summary = "更新活动状态")
    @PutMapping("/campaigns/{id}/status")
    public Result<Void> updateCampaignStatus(
            @Parameter(description = "活动ID") @PathVariable Long id,
            @RequestBody CampaignStatusUpdateRequest request) {
        adminMarketingService.updateCampaignStatus(id, request);
        return Result.success();
    }

    @Operation(summary = "查询促销规则")
    @GetMapping("/promotion-rules")
    public Result<PageResponse<Object>> listPromotionRules(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") Integer pageSize) {
        PageResponse<Object> page = adminMarketingService.listPromotionRules(pageNum, pageSize);
        return Result.success(page);
    }
}