package com.tailoris.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tailoris.api.admin.dto.ApplyTypeDTO;
import com.tailoris.api.admin.dto.ApplyTypeQueryRequest;
import com.tailoris.admin.service.AdminApplyTypeService;
import com.tailoris.common.dto.PageResponse;
import com.tailoris.common.exception.BusinessException;
import com.tailoris.api.merchant.entity.ApplyType;
import com.tailoris.api.merchant.entity.MerchantModifyLog;
import com.tailoris.api.merchant.mapper.ApplyTypeMapper;
import com.tailoris.api.merchant.mapper.MerchantModifyLogMapper;
import com.tailoris.common.util.AdminSecurityUtils;
import com.tailoris.common.util.HttpRequestUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminApplyTypeServiceImpl implements AdminApplyTypeService {

    private final ApplyTypeMapper applyTypeMapper;
    private final MerchantModifyLogMapper merchantModifyLogMapper;
    private final ObjectMapper objectMapper;
    private final HttpServletRequest request;

    @Override
    public PageResponse<ApplyType> page(ApplyTypeQueryRequest req) {
        LambdaQueryWrapper<ApplyType> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(req.getKeyword())) {
            wrapper.and(w -> w.like(ApplyType::getTypeName, req.getKeyword())
                    .or().like(ApplyType::getTypeCode, req.getKeyword()));
        }
        if (req.getStatus() != null) {
            wrapper.eq(ApplyType::getStatus, req.getStatus());
        }
        wrapper.orderByAsc(ApplyType::getSortOrder).orderByDesc(ApplyType::getCreateTime);

        Page<ApplyType> page = new Page<>(req.getPageNum(), req.getPageSize());
        applyTypeMapper.selectPage(page, wrapper);
        return PageResponse.of(page);
    }

    @Override
    public java.util.List<ApplyType> listEnabled() {
        LambdaQueryWrapper<ApplyType> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ApplyType::getStatus, 1).orderByAsc(ApplyType::getSortOrder);
        return applyTypeMapper.selectList(wrapper);
    }

    @Override
    public ApplyType getById(Long id) {
        ApplyType applyType = applyTypeMapper.selectById(id);
        if (applyType == null) {
            throw new BusinessException("入驻类型不存在");
        }
        return applyType;
    }

    @Override
    public void create(ApplyTypeDTO dto, Long adminId) {
        ApplyType applyType = objectMapper.convertValue(dto, ApplyType.class);
        if (dto.getRequiredQualifications() != null) {
            try {
                applyType.setRequiredQualifications(objectMapper.writeValueAsString(dto.getRequiredQualifications()));
            } catch (Exception e) {
                log.warn("序列化资质列表失败", e);
            }
        }
        if (applyType.getStatus() == null) {
            applyType.setStatus(1);
        }
        if (applyType.getSortOrder() == null) {
            applyType.setSortOrder(0);
        }
        String operatorName = adminId == null || adminId < 0 ? "system" : String.valueOf(adminId);
        applyType.setCreateBy(operatorName);
        applyType.setUpdateBy(operatorName);
        applyTypeMapper.insert(applyType);

        writeModifyLog(adminId, null, applyType.getId(), 1, "apply_type",
                null, objectToJson(dto), "新增入驻类型: " + applyType.getTypeName());
        log.info("新增入驻类型成功, id: {}, name: {}, adminId: {}", applyType.getId(), applyType.getTypeName(), adminId);
    }

    @Override
    public void update(Long id, ApplyTypeDTO dto, Long adminId) {
        ApplyType old = getById(id);
        String before = objectToJson(old);

        ApplyType applyType = objectMapper.convertValue(dto, ApplyType.class);
        if (dto.getRequiredQualifications() != null) {
            try {
                applyType.setRequiredQualifications(objectMapper.writeValueAsString(dto.getRequiredQualifications()));
            } catch (Exception e) {
                log.warn("序列化资质列表失败", e);
            }
        }
        applyType.setId(id);
        String operatorName = adminId == null || adminId < 0 ? "system" : String.valueOf(adminId);
        applyType.setUpdateBy(operatorName);
        applyTypeMapper.updateById(applyType);

        writeModifyLog(adminId, null, id, 2, "apply_type",
                before, objectToJson(dto), "修改入驻类型: " + old.getTypeName());
        log.info("修改入驻类型成功, id: {}, adminId: {}", id, adminId);
    }

    @Override
    public void remove(Long id, Long adminId) {
        ApplyType old = getById(id);
        applyTypeMapper.deleteById(id);

        writeModifyLog(adminId, null, id, 3, "apply_type",
                objectToJson(old), null, "删除入驻类型: " + old.getTypeName());
        log.info("删除入驻类型成功, id: {}, adminId: {}", id, adminId);
    }

    private String objectToJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            log.warn("对象转JSON失败", e);
            return null;
        }
    }

    private void writeModifyLog(Long operatorId, Long merchantId, Long shopId, Integer operateType,
                                String operateModule, String valueBefore, String valueAfter, String remark) {
        try {
            MerchantModifyLog logEntity = new MerchantModifyLog();
            logEntity.setMerchantId(merchantId);
            logEntity.setShopId(shopId);
            logEntity.setOperatorId(operatorId != null && operatorId > 0 ? operatorId : AdminSecurityUtils.UNKNOWN_OPERATOR);
            logEntity.setOperatorType(AdminSecurityUtils.OPERATOR_TYPE_ADMIN);
            logEntity.setOperateType(operateType);
            logEntity.setOperateModule(operateModule);
            logEntity.setValueBefore(AdminSecurityUtils.safeTruncate(valueBefore, 4000));
            logEntity.setValueAfter(AdminSecurityUtils.safeTruncate(valueAfter, 4000));
            logEntity.setRemark(AdminSecurityUtils.safeTruncate(remark, 512));
            String ip = request != null ? HttpRequestUtils.getClientIp(request) : AdminSecurityUtils.getCurrentIp();
            logEntity.setIp(AdminSecurityUtils.safeTruncate(ip, 64));
            String ua = request != null ? request.getHeader("User-Agent") : AdminSecurityUtils.getCurrentUserAgent();
            logEntity.setUserAgent(AdminSecurityUtils.safeTruncate(ua, 512));
            merchantModifyLogMapper.insert(logEntity);
        } catch (Exception e) {
            log.error("写入修改日志失败 module={}", operateModule, e);
        }
    }
}
