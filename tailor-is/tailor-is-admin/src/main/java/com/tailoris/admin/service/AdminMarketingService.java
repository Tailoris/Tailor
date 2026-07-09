package com.tailoris.admin.service;

import com.tailoris.admin.dto.CampaignStatusUpdateRequest;
import com.tailoris.admin.dto.CouponCreateRequest;
import com.tailoris.admin.dto.CouponQueryRequest;
import com.tailoris.common.dto.PageResponse;

public interface AdminMarketingService {

    PageResponse<Object> listCoupons(CouponQueryRequest request);

    Long createCoupon(CouponCreateRequest request);

    void disableCoupon(Long id);

    PageResponse<Object> listCampaigns(Integer status, Integer pageNum, Integer pageSize);

    void updateCampaignStatus(Long id, CampaignStatusUpdateRequest request);

    PageResponse<Object> listPromotionRules(Integer pageNum, Integer pageSize);
}