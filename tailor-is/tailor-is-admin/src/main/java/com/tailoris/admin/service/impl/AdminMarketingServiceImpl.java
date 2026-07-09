package com.tailoris.admin.service.impl;

import com.tailoris.admin.dto.CampaignStatusUpdateRequest;
import com.tailoris.admin.dto.CouponCreateRequest;
import com.tailoris.admin.dto.CouponQueryRequest;
import com.tailoris.admin.feign.MarketingFeignClient;
import com.tailoris.admin.service.AdminMarketingService;
import com.tailoris.common.dto.PageResponse;
import com.tailoris.common.exception.BusinessException;
import com.tailoris.common.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminMarketingServiceImpl implements AdminMarketingService {

    private final MarketingFeignClient marketingFeignClient;

    @Override
    public PageResponse<Object> listCoupons(CouponQueryRequest request) {
        Result<PageResponse<Object>> response = marketingFeignClient.getCouponList(request);
        if (!response.isSuccess()) {
            throw new BusinessException(response.getMessage());
        }
        return response.getData();
    }

    @Override
    public Long createCoupon(CouponCreateRequest request) {
        Result<Long> response = marketingFeignClient.createCoupon(request);
        if (!response.isSuccess()) {
            throw new BusinessException(response.getMessage());
        }
        return response.getData();
    }

    @Override
    public void disableCoupon(Long id) {
        Result<Void> response = marketingFeignClient.disableCoupon(id);
        if (!response.isSuccess()) {
            throw new BusinessException(response.getMessage());
        }
    }

    @Override
    public PageResponse<Object> listCampaigns(Integer status, Integer pageNum, Integer pageSize) {
        Result<PageResponse<Object>> response = marketingFeignClient.getCampaignList();
        if (!response.isSuccess()) {
            throw new BusinessException(response.getMessage());
        }
        return response.getData();
    }

    @Override
    public void updateCampaignStatus(Long id, CampaignStatusUpdateRequest request) {
        Result<Void> response = marketingFeignClient.updateCampaignStatus(id, request);
        if (!response.isSuccess()) {
            throw new BusinessException(response.getMessage());
        }
    }

    @Override
    public PageResponse<Object> listPromotionRules(Integer pageNum, Integer pageSize) {
        Result<PageResponse<Object>> response = marketingFeignClient.getPromotionRules();
        if (!response.isSuccess()) {
            throw new BusinessException(response.getMessage());
        }
        return response.getData();
    }
}