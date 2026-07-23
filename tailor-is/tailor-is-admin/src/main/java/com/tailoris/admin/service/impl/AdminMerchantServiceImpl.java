package com.tailoris.admin.service.impl;

import com.tailoris.admin.constant.AdminConstants;
import com.tailoris.api.admin.dto.MerchantAuditRequest;
import com.tailoris.api.admin.dto.MerchantQueryRequest;
import com.tailoris.admin.feign.MerchantFeignClient;
import com.tailoris.admin.service.AdminMerchantService;
import com.tailoris.common.dto.PageResponse;
import com.tailoris.common.exception.BusinessException;
import com.tailoris.common.result.Result;
import com.tailoris.api.merchant.entity.Merchant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminMerchantServiceImpl implements AdminMerchantService {

    private final MerchantFeignClient merchantFeignClient;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public PageResponse<Merchant> listPendingMerchants(MerchantQueryRequest request) {
        request.setAuditStatus(AdminConstants.AUDIT_STATUS_PENDING);
        return listMerchants(request);
    }

    @Override
    public void auditMerchant(MerchantAuditRequest request, Long adminId) {
        Result<Merchant> result = merchantFeignClient.getMerchantById(request.getMerchantId());
        if (!result.isSuccess() || result.getData() == null) {
            throw new BusinessException("商家不存在");
        }

        Merchant merchant = result.getData();
        if (merchant.getAuditStatus().equals(AdminConstants.AUDIT_STATUS_APPROVED)) {
            throw new BusinessException("商家已通过审核，无需重复操作");
        }

        Map<String, Object> auditRequest = new HashMap<>();
        auditRequest.put("merchantId", request.getMerchantId());
        auditRequest.put("auditStatus", request.getAuditStatus());
        auditRequest.put("auditRemark", request.getAuditRemark());

        Result<Void> auditResult = merchantFeignClient.auditMerchant(auditRequest);
        if (!auditResult.isSuccess()) {
            throw new BusinessException(auditResult.getMessage());
        }

        stringRedisTemplate.delete("tailoris:merchant:info:" + merchant.getId());
        stringRedisTemplate.delete("tailoris:merchant:info:user:" + merchant.getUserId());
        stringRedisTemplate.delete(AdminConstants.REDIS_KEY_DASHBOARD_STATS);

        log.info("商家审核完成, merchantId: {}, auditStatus: {}", request.getMerchantId(), request.getAuditStatus());
    }

    @Override
    public void freezeMerchant(Long merchantId) {
        Result<Merchant> result = merchantFeignClient.getMerchantById(merchantId);
        if (!result.isSuccess() || result.getData() == null) {
            throw new BusinessException("商家不存在");
        }

        Merchant merchant = result.getData();
        if (merchant.getStatus().equals(AdminConstants.MERCHANT_STATUS_FROZEN)) {
            throw new BusinessException("商家已处于冻结状态");
        }

        Map<String, Object> auditRequest = new HashMap<>();
        auditRequest.put("merchantId", merchantId);
        auditRequest.put("auditStatus", AdminConstants.MERCHANT_STATUS_FROZEN);

        Result<Void> auditResult = merchantFeignClient.auditMerchant(auditRequest);
        if (!auditResult.isSuccess()) {
            throw new BusinessException(auditResult.getMessage());
        }

        stringRedisTemplate.delete("tailoris:merchant:info:" + merchantId);
        log.info("商家已冻结, merchantId: {}", merchantId);
    }

    @Override
    public void unfreezeMerchant(Long merchantId) {
        Result<Merchant> result = merchantFeignClient.getMerchantById(merchantId);
        if (!result.isSuccess() || result.getData() == null) {
            throw new BusinessException("商家不存在");
        }

        Merchant merchant = result.getData();
        if (!merchant.getStatus().equals(AdminConstants.MERCHANT_STATUS_FROZEN)) {
            throw new BusinessException("商家未处于冻结状态");
        }

        Map<String, Object> auditRequest = new HashMap<>();
        auditRequest.put("merchantId", merchantId);
        auditRequest.put("auditStatus", AdminConstants.MERCHANT_STATUS_NORMAL);

        Result<Void> auditResult = merchantFeignClient.auditMerchant(auditRequest);
        if (!auditResult.isSuccess()) {
            throw new BusinessException(auditResult.getMessage());
        }

        stringRedisTemplate.delete("tailoris:merchant:info:" + merchantId);
        log.info("商家已解冻, merchantId: {}", merchantId);
    }

    @Override
    public PageResponse<Merchant> listMerchants(MerchantQueryRequest request) {
        Map<String, Object> params = new HashMap<>();
        params.put("pageNum", request.getPageNum());
        params.put("pageSize", request.getPageSize());

        if (request.getMerchantType() != null) {
            params.put("merchantType", request.getMerchantType());
        }
        if (request.getAuditStatus() != null) {
            params.put("auditStatus", request.getAuditStatus());
        }
        if (request.getStatus() != null) {
            params.put("status", request.getStatus());
        }
        if (StringUtils.hasText(request.getKeyword())) {
            params.put("keyword", request.getKeyword());
        }

        Result<PageResponse<Merchant>> result = merchantFeignClient.listMerchants(params);
        if (!result.isSuccess()) {
            throw new BusinessException(result.getMessage());
        }

        return result.getData();
    }

    @Override
    public Merchant getMerchantDetail(Long merchantId) {
        Result<Merchant> result = merchantFeignClient.getMerchantById(merchantId);
        if (!result.isSuccess() || result.getData() == null) {
            throw new BusinessException("商家不存在");
        }
        return result.getData();
    }
}