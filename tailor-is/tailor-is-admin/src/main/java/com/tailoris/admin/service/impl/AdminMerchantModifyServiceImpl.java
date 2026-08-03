package com.tailoris.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tailoris.api.admin.dto.MerchantModifyApplyAuditRequest;
import com.tailoris.api.admin.dto.MerchantModifyApplyQueryRequest;
import com.tailoris.admin.service.AdminMerchantModifyService;
import com.tailoris.common.dto.PageResponse;
import com.tailoris.common.exception.BusinessException;
import com.tailoris.api.merchant.entity.Merchant;
import com.tailoris.api.merchant.entity.MerchantModifyApply;
import com.tailoris.api.merchant.entity.MerchantModifyLog;
import com.tailoris.api.merchant.mapper.MerchantMapper;
import com.tailoris.api.merchant.mapper.MerchantModifyApplyMapper;
import com.tailoris.api.merchant.mapper.MerchantModifyLogMapper;
import com.tailoris.common.util.AdminSecurityUtils;
import com.tailoris.common.util.HttpRequestUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminMerchantModifyServiceImpl implements AdminMerchantModifyService {

    private static final Set<String> MODIFY_FIELD_WHITELIST = new HashSet<>(Arrays.asList(
            "contact_name", "contact_phone", "contact_email",
            "province", "city", "district", "address", "business_scope",
            "shop_category_id", "apply_type_code"
    ));

    private final MerchantModifyApplyMapper applyMapper;
    private final MerchantModifyLogMapper modifyLogMapper;
    private final MerchantMapper merchantMapper;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final HttpServletRequest request;

    @Value("${tailoris.datasource.merchant-db:tailor_is_merchant}")
    private String merchantDatabase;

    @Override
    public PageResponse<MerchantModifyApply> page(MerchantModifyApplyQueryRequest req) {
        LambdaQueryWrapper<MerchantModifyApply> wrapper = new LambdaQueryWrapper<>();
        if (req.getMerchantId() != null) {
            wrapper.eq(MerchantModifyApply::getMerchantId, req.getMerchantId());
        }
        if (req.getApplyType() != null) {
            wrapper.eq(MerchantModifyApply::getApplyType, req.getApplyType());
        }
        if (req.getStatus() != null) {
            wrapper.eq(MerchantModifyApply::getStatus, req.getStatus());
        }
        if (req.getStartDate() != null) {
            wrapper.ge(MerchantModifyApply::getCreateTime, req.getStartDate());
        }
        if (req.getEndDate() != null) {
            wrapper.le(MerchantModifyApply::getCreateTime, req.getEndDate());
        }
        if (StringUtils.hasText(req.getKeyword())) {
            wrapper.like(MerchantModifyApply::getApplyNo, req.getKeyword());
        }
        wrapper.orderByDesc(MerchantModifyApply::getCreateTime);

        Page<MerchantModifyApply> page = new Page<>(req.getPageNum(), req.getPageSize());
        applyMapper.selectPage(page, wrapper);
        return PageResponse.of(page);
    }

    @Override
    public MerchantModifyApply detail(Long applyId) {
        MerchantModifyApply apply = applyMapper.selectById(applyId);
        if (apply == null) {
            throw new BusinessException("修改申请不存在");
        }
        return apply;
    }

    @Override
    public void audit(MerchantModifyApplyAuditRequest req, Long adminId) {
        MerchantModifyApply apply = detail(req.getApplyId());
        if (apply.getStatus() != null && apply.getStatus() != 0 && apply.getStatus() != 1) {
            throw new BusinessException("该申请已审核，无法重复操作");
        }

        Merchant merchant = merchantMapper.selectById(apply.getMerchantId());
        if (merchant == null) {
            throw new BusinessException("关联商家不存在");
        }

        if (Boolean.TRUE.equals(req.getApproved())) {
            apply.setStatus(2);
            apply.setAuditRemark(req.getRemark());
            apply.setAuditBy(adminId);
            apply.setAuditTime(LocalDateTime.now());
            applyMapper.updateById(apply);

            Map<String, Object> modifyContent = parseModifyContent(apply.getModifyContent());
            if (modifyContent != null && !modifyContent.isEmpty()) {
                applyModifyToMerchant(apply.getMerchantId(), modifyContent, merchant, adminId);
            }

            writeModifyLog(adminId, apply.getMerchantId(), apply.getShopId(), 6,
                    "merchant_modify_apply", null, null, apply.getModifyContent(),
                    "审核通过商家修改申请: " + apply.getApplyNo());
            log.info("商家修改申请审核通过, applyId: {}, merchantId: {}, adminId: {}", req.getApplyId(), apply.getMerchantId(), adminId);
        } else {
            apply.setStatus(3);
            apply.setAuditRemark(req.getRemark());
            apply.setAuditBy(adminId);
            apply.setAuditTime(LocalDateTime.now());
            applyMapper.updateById(apply);

            writeModifyLog(adminId, apply.getMerchantId(), apply.getShopId(), 7,
                    "merchant_modify_apply", null, null, null,
                    "审核驳回商家修改申请: " + apply.getApplyNo() + ", 原因: " + req.getRemark());
            log.info("商家修改申请审核驳回, applyId: {}, merchantId: {}, adminId: {}", req.getApplyId(), apply.getMerchantId(), adminId);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseModifyContent(String json) {
        if (!StringUtils.hasText(json)) {
            return null;
        }
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (Exception e) {
            log.warn("解析modify_content失败", e);
            return null;
        }
    }

    private void applyModifyToMerchant(Long merchantId, Map<String, Object> modifyContent, Merchant merchantBefore, Long adminId) {
        List<String> setClauses = new ArrayList<>();
        List<Object> params = new ArrayList<>();
        Map<String, String> fieldChanges = new LinkedHashMap<>();

        for (Map.Entry<String, Object> entry : modifyContent.entrySet()) {
            String field = toSnakeCase(entry.getKey());
            if (!MODIFY_FIELD_WHITELIST.contains(field)) {
                log.warn("跳过非白名单字段: {}", field);
                continue;
            }
            setClauses.add(field + " = ?");
            params.add(entry.getValue());
            fieldChanges.put(field, entry.getValue() != null ? entry.getValue().toString() : null);
        }

        if (setClauses.isEmpty()) {
            return;
        }

        params.add(merchantId);
        String sql = "UPDATE " + merchantDatabase + ".merchant SET " + String.join(", ", setClauses) + " WHERE id = ?";

        try {
            jdbcTemplate.update(sql, params.toArray());
            for (Map.Entry<String, String> change : fieldChanges.entrySet()) {
                String beforeVal = getMerchantFieldValue(merchantBefore, change.getKey());
                writeModifyLog(adminId, merchantId, null, 2,
                        "merchant", change.getKey(), beforeVal, change.getValue(),
                        "商家信息修改（申请审核通过）");
            }
        } catch (Exception e) {
            log.error("更新商家信息失败, merchantId: {}", merchantId, e);
            throw new BusinessException("更新商家信息失败");
        }
    }

    private String getMerchantFieldValue(Merchant merchant, String field) {
        try {
            java.lang.reflect.Field f = Merchant.class.getDeclaredField(field);
            f.setAccessible(true);
            Object val = f.get(merchant);
            return val != null ? val.toString() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private String toSnakeCase(String camel) {
        if (camel == null) return null;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < camel.length(); i++) {
            char c = camel.charAt(i);
            if (Character.isUpperCase(c)) {
                if (i > 0) sb.append('_');
                sb.append(Character.toLowerCase(c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private void writeModifyLog(Long operatorId, Long merchantId, Long shopId, Integer operateType,
                                String operateModule, String fieldName, String valueBefore, String valueAfter, String remark) {
        try {
            MerchantModifyLog logEntity = new MerchantModifyLog();
            logEntity.setMerchantId(merchantId);
            logEntity.setShopId(shopId);
            logEntity.setOperatorId(operatorId != null && operatorId > 0 ? operatorId : AdminSecurityUtils.UNKNOWN_OPERATOR);
            logEntity.setOperatorType(AdminSecurityUtils.OPERATOR_TYPE_ADMIN);
            logEntity.setOperateType(operateType);
            logEntity.setOperateModule(operateModule);
            logEntity.setFieldName(AdminSecurityUtils.safeTruncate(fieldName, 128));
            logEntity.setValueBefore(AdminSecurityUtils.safeTruncate(valueBefore, 4000));
            logEntity.setValueAfter(AdminSecurityUtils.safeTruncate(valueAfter, 4000));
            logEntity.setRemark(AdminSecurityUtils.safeTruncate(remark, 512));
            String ip = request != null ? HttpRequestUtils.getClientIp(request) : AdminSecurityUtils.getCurrentIp();
            logEntity.setIp(AdminSecurityUtils.safeTruncate(ip, 64));
            String ua = request != null ? request.getHeader("User-Agent") : AdminSecurityUtils.getCurrentUserAgent();
            logEntity.setUserAgent(AdminSecurityUtils.safeTruncate(ua, 512));
            modifyLogMapper.insert(logEntity);
        } catch (Exception e) {
            log.error("写入修改日志失败", e);
        }
    }
}
