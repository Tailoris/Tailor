package com.tailoris.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tailoris.admin.constant.AdminConstants;
import com.tailoris.api.admin.dto.MerchantAuditRequest;
import com.tailoris.api.admin.dto.MerchantQueryRequest;
import com.tailoris.admin.service.AdminDashboardService;
import com.tailoris.admin.service.AdminMerchantService;
import com.tailoris.common.dto.PageResponse;
import com.tailoris.common.exception.BusinessException;
import com.tailoris.api.merchant.entity.Merchant;
import com.tailoris.api.merchant.mapper.MerchantMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminMerchantServiceImpl implements AdminMerchantService {

    private final MerchantMapper merchantMapper;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public PageResponse<Merchant> listPendingMerchants(MerchantQueryRequest request) {
        request.setAuditStatus(AdminConstants.AUDIT_STATUS_PENDING);
        return listMerchants(request);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditMerchant(MerchantAuditRequest request, Long adminId) {
        Merchant merchant = merchantMapper.selectById(request.getMerchantId());
        if (merchant == null) {
            throw new BusinessException("商家不存在");
        }

        if (merchant.getAuditStatus().equals(AdminConstants.AUDIT_STATUS_APPROVED)) {
            throw new BusinessException("商家已通过审核，无需重复操作");
        }

        merchant.setAuditStatus(request.getAuditStatus());
        merchant.setAuditRemark(request.getAuditRemark());
        merchant.setAuditTime(LocalDateTime.now());
        merchant.setAuditBy(adminId);

        if (request.getAuditStatus().equals(AdminConstants.AUDIT_STATUS_APPROVED)) {
            merchant.setStatus(AdminConstants.MERCHANT_STATUS_NORMAL);
            merchant.setJoinTime(LocalDateTime.now());
        } else if (request.getAuditStatus().equals(AdminConstants.AUDIT_STATUS_REJECTED)) {
            merchant.setStatus(AdminConstants.MERCHANT_STATUS_PENDING);
        }

        merchantMapper.updateById(merchant);

        stringRedisTemplate.delete("tailoris:merchant:info:" + merchant.getId());
        stringRedisTemplate.delete("tailoris:merchant:info:user:" + merchant.getUserId());
        stringRedisTemplate.delete(AdminConstants.REDIS_KEY_DASHBOARD_STATS);

        log.info("商家审核完成, merchantId: {}, auditStatus: {}", request.getMerchantId(), request.getAuditStatus());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void freezeMerchant(Long merchantId) {
        Merchant merchant = merchantMapper.selectById(merchantId);
        if (merchant == null) {
            throw new BusinessException("商家不存在");
        }
        if (merchant.getStatus().equals(AdminConstants.MERCHANT_STATUS_FROZEN)) {
            throw new BusinessException("商家已处于冻结状态");
        }
        merchant.setStatus(AdminConstants.MERCHANT_STATUS_FROZEN);
        merchantMapper.updateById(merchant);
        stringRedisTemplate.delete("tailoris:merchant:info:" + merchantId);
        log.info("商家已冻结, merchantId: {}", merchantId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unfreezeMerchant(Long merchantId) {
        Merchant merchant = merchantMapper.selectById(merchantId);
        if (merchant == null) {
            throw new BusinessException("商家不存在");
        }
        if (!merchant.getStatus().equals(AdminConstants.MERCHANT_STATUS_FROZEN)) {
            throw new BusinessException("商家未处于冻结状态");
        }
        merchant.setStatus(AdminConstants.MERCHANT_STATUS_NORMAL);
        merchantMapper.updateById(merchant);
        stringRedisTemplate.delete("tailoris:merchant:info:" + merchantId);
        log.info("商家已解冻, merchantId: {}", merchantId);
    }

    @Override
    public PageResponse<Merchant> listMerchants(MerchantQueryRequest request) {
        LambdaQueryWrapper<Merchant> queryWrapper = new LambdaQueryWrapper<>();

        if (request.getMerchantType() != null) {
            queryWrapper.eq(Merchant::getMerchantType, request.getMerchantType());
        }
        if (request.getAuditStatus() != null) {
            queryWrapper.eq(Merchant::getAuditStatus, request.getAuditStatus());
        }
        if (request.getStatus() != null) {
            queryWrapper.eq(Merchant::getStatus, request.getStatus());
        }
        if (StringUtils.hasText(request.getKeyword())) {
            queryWrapper.and(wrapper -> wrapper
                    .like(Merchant::getCompanyName, request.getKeyword())
                    .or()
                    .like(Merchant::getContactName, request.getKeyword())
            );
        }

        queryWrapper.orderByDesc(Merchant::getCreateTime);

        Page<Merchant> page = new Page<>(request.getPageNum(), request.getPageSize());
        Page<Merchant> result = merchantMapper.selectPage(page, queryWrapper);

        return new PageResponse<>(result.getRecords(), result.getTotal(), request.getPageNum(), request.getPageSize());
    }

    @Override
    public Merchant getMerchantDetail(Long merchantId) {
        Merchant merchant = merchantMapper.selectById(merchantId);
        if (merchant == null) {
            throw new BusinessException("商家不存在");
        }
        return merchant;
    }
}
