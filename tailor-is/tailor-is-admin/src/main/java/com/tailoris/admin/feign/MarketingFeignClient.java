package com.tailoris.admin.feign;

import com.tailoris.admin.dto.CampaignStatusUpdateRequest;
import com.tailoris.admin.dto.CouponCreateRequest;
import com.tailoris.admin.dto.CouponQueryRequest;
import com.tailoris.common.dto.PageResponse;
import com.tailoris.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "tailor-is-marketing")
public interface MarketingFeignClient {

    @GetMapping("/api/v1/marketing/coupons")
    Result<PageResponse<Object>> getCouponList(@RequestBody CouponQueryRequest request);

    @PostMapping("/api/v1/marketing/coupons")
    Result<Long> createCoupon(@RequestBody CouponCreateRequest request);

    @PutMapping("/api/v1/marketing/coupons/{id}/disable")
    Result<Void> disableCoupon(@PathVariable Long id);

    @GetMapping("/api/v1/marketing/campaigns")
    Result<PageResponse<Object>> getCampaignList();

    @PutMapping("/api/v1/marketing/campaigns/{id}/status")
    Result<Void> updateCampaignStatus(@PathVariable Long id, @RequestBody CampaignStatusUpdateRequest request);

    @GetMapping("/api/v1/marketing/rules")
    Result<PageResponse<Object>> getPromotionRules();
}