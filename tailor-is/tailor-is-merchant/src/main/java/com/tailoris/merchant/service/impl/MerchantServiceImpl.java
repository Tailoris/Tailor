package com.tailoris.merchant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tailoris.common.dto.PageResponse;
import com.tailoris.common.exception.BusinessException;
import com.tailoris.merchant.constant.MerchantConstants;
import com.tailoris.merchant.dto.MerchantApplyRequest;
import com.tailoris.merchant.dto.MerchantAuditRequest;
import com.tailoris.merchant.dto.MerchantQueryRequest;
import com.tailoris.merchant.entity.Merchant;
import com.tailoris.merchant.entity.MerchantQualification;
import com.tailoris.merchant.mapper.MerchantMapper;
import com.tailoris.merchant.mapper.MerchantQualificationMapper;
import com.tailoris.merchant.service.MerchantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.tailoris.merchant.constant.MerchantConstants.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class MerchantServiceImpl implements MerchantService {

    private final MerchantMapper merchantMapper;
    private final MerchantQualificationMapper merchantQualificationMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void applyJoin(Long userId, MerchantApplyRequest request) {
        LambdaQueryWrapper<Merchant> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Merchant::getUserId, userId);
        Merchant existingMerchant = merchantMapper.selectOne(queryWrapper);
        if (existingMerchant != null) {
            throw new BusinessException("您已提交过入驻申请，请勿重复提交");
        }

        Merchant merchant = new Merchant();
        merchant.setUserId(userId);
        merchant.setMerchantType(request.getMerchantType());
        merchant.setCompanyName(request.getCompanyName());
        merchant.setLicenseNo(request.getLicenseNo());
        merchant.setContactName(request.getContactName());
        merchant.setContactPhone(request.getContactPhone());
        merchant.setContactEmail(request.getContactEmail());
        merchant.setProvince(request.getProvince());
        merchant.setCity(request.getCity());
        merchant.setDistrict(request.getDistrict());
        merchant.setAddress(request.getAddress());
        merchant.setBusinessScope(request.getBusinessScope());
        merchant.setStatus(MERCHANT_STATUS_PENDING);
        merchant.setAuditStatus(MERCHANT_AUDIT_STATUS_PENDING);

        merchantMapper.insert(merchant);

        if (request.getQualifications() != null && !request.getQualifications().isEmpty()) {
            List<MerchantQualification> qualificationList = new ArrayList<>();
            for (MerchantApplyRequest.QualificationItem item : request.getQualifications()) {
                MerchantQualification qualification = new MerchantQualification();
                qualification.setMerchantId(merchant.getId());
                qualification.setCertType(item.getCertType());
                qualification.setCertName(item.getCertName());
                qualification.setCertNo(item.getCertNo());
                qualification.setCertUrl(item.getCertUrl());
                qualification.setCertFrontUrl(item.getCertFrontUrl());
                qualification.setCertBackUrl(item.getCertBackUrl());
                qualification.setAuditStatus(QUALIFICATION_AUDIT_STATUS_PENDING);
                qualificationList.add(qualification);
            }
            merchantQualificationMapper.insertBatchSomeColumn(qualificationList);
        }

        log.info("商家入驻申请提交成功, userId: {}, merchantId: {}", userId, merchant.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void audit(MerchantAuditRequest request) {
        Merchant merchant = merchantMapper.selectById(request.getMerchantId());
        if (merchant == null) {
            throw new BusinessException("商家不存在");
        }

        merchant.setAuditStatus(request.getAuditStatus());
        merchant.setAuditRemark(request.getAuditRemark());
        merchant.setAuditTime(LocalDateTime.now());

        if (request.getAuditStatus().equals(MERCHANT_AUDIT_STATUS_APPROVED)) {
            merchant.setStatus(MERCHANT_STATUS_NORMAL);
            merchant.setJoinTime(LocalDateTime.now());
        } else if (request.getAuditStatus().equals(MERCHANT_AUDIT_STATUS_REJECTED)) {
            merchant.setStatus(MERCHANT_STATUS_PENDING);
        }

        merchantMapper.updateById(merchant);

        StringRedisTemplate ops = stringRedisTemplate;
        ops.delete(REDIS_KEY_MERCHANT_INFO + merchant.getId());
        ops.delete(REDIS_KEY_MERCHANT_INFO + "user:" + merchant.getUserId());

        log.info("商家审核完成, merchantId: {}, auditStatus: {}", request.getMerchantId(), request.getAuditStatus());
    }

    @Override
    public Merchant getMerchantInfo(Long userId) {
        String cacheKey = REDIS_KEY_MERCHANT_INFO + "user:" + userId;
        String cached = stringRedisTemplate.opsForValue().get(cacheKey);
        if (StringUtils.hasText(cached)) {
            try {
                return objectMapper.readValue(cached, Merchant.class);
            } catch (Exception e) {
                log.warn("Redis反序列化失败, key: {}", cacheKey);
            }
        }

        Merchant merchant = getMerchantByUserId(userId);
        if (merchant != null) {
            try {
                stringRedisTemplate.opsForValue().set(
                        cacheKey,
                        objectMapper.writeValueAsString(merchant),
                        REDIS_EXPIRE_MINUTES,
                        TimeUnit.MINUTES
                );
            } catch (JsonProcessingException e) {
                log.warn("Redis序列化失败, key: {}", cacheKey);
            }
        }
        return merchant;
    }

    @Override
    public Merchant getMerchantById(Long merchantId) {
        String cacheKey = REDIS_KEY_MERCHANT_INFO + merchantId;
        String cached = stringRedisTemplate.opsForValue().get(cacheKey);
        if (StringUtils.hasText(cached)) {
            try {
                return objectMapper.readValue(cached, Merchant.class);
            } catch (Exception e) {
                log.warn("Redis反序列化失败, key: {}", cacheKey);
            }
        }

        Merchant merchant = merchantMapper.selectById(merchantId);
        if (merchant != null) {
            try {
                stringRedisTemplate.opsForValue().set(
                        cacheKey,
                        objectMapper.writeValueAsString(merchant),
                        REDIS_EXPIRE_MINUTES,
                        TimeUnit.MINUTES
                );
            } catch (JsonProcessingException e) {
                log.warn("Redis序列化失败, key: {}", cacheKey);
            }
        }
        return merchant;
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
    public Merchant getMerchantByUserId(Long userId) {
        LambdaQueryWrapper<Merchant> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Merchant::getUserId, userId);
        return merchantMapper.selectOne(queryWrapper);
    }
}
