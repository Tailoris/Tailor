package com.tailoris.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tailoris.api.admin.dto.DepositAdjustRequest;
import com.tailoris.api.admin.dto.DepositFlowQueryRequest;
import com.tailoris.api.admin.dto.DepositQueryRequest;
import com.tailoris.admin.service.AdminDepositService;
import com.tailoris.common.dto.PageResponse;
import com.tailoris.common.exception.BusinessException;
import com.tailoris.api.merchant.entity.DepositFlow;
import com.tailoris.api.merchant.entity.MerchantModifyLog;
import com.tailoris.api.merchant.mapper.DepositFlowMapper;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminDepositServiceImpl implements AdminDepositService {

    private final JdbcTemplate jdbcTemplate;
    private final DepositFlowMapper depositFlowMapper;
    private final MerchantModifyLogMapper modifyLogMapper;
    private final ObjectMapper objectMapper;
    private final HttpServletRequest request;

    @Value("${tailoris.datasource.merchant-db:tailor_is_merchant}")
    private String merchantDatabase;

    @Value("${tailoris.datasource.payment-db:tailor_is_payment}")
    private String paymentDatabase;

    @Override
    public PageResponse<Map<String, Object>> page(DepositQueryRequest req) {
        StringBuilder sql = new StringBuilder("SELECT qd.*, m.company_name AS merchant_name, m.contact_name, m.contact_phone ")
                .append("FROM ").append(paymentDatabase).append(".quality_deposit qd ")
                .append("LEFT JOIN ").append(merchantDatabase).append(".merchant m ON qd.merchant_id = m.id ")
                .append("WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (req.getMerchantId() != null) {
            sql.append(" AND qd.merchant_id = ?");
            params.add(req.getMerchantId());
        }
        if (req.getStatus() != null) {
            sql.append(" AND qd.status = ?");
            params.add(req.getStatus());
        }
        if (req.getMinDeposit() != null) {
            sql.append(" AND qd.deposit_amount >= ?");
            params.add(req.getMinDeposit());
        }
        if (req.getMaxDeposit() != null) {
            sql.append(" AND qd.deposit_amount <= ?");
            params.add(req.getMaxDeposit());
        }

        String countSql = "SELECT COUNT(*) FROM (" + sql + ") t";
        Long total = jdbcTemplate.queryForObject(countSql, Long.class, params.toArray());
        if (total == null) total = 0L;

        sql.append(" ORDER BY qd.created_at DESC LIMIT ? OFFSET ?");
        params.add(req.getPageSize());
        params.add(req.getOffset());

        List<Map<String, Object>> records = jdbcTemplate.queryForList(sql.toString(), params.toArray());
        return PageResponse.of(records, total, req.getPageNum(), req.getPageSize());
    }

    @Override
    public Map<String, Object> getMerchantDepositDetail(Long merchantId) {
        Map<String, Object> result = new HashMap<>();

        try {
            String depositSql = "SELECT * FROM " + paymentDatabase + ".quality_deposit WHERE merchant_id = ? LIMIT 1";
            List<Map<String, Object>> depositList = jdbcTemplate.queryForList(depositSql, merchantId);
            if (!depositList.isEmpty()) {
                result.put("deposit", depositList.get(0));
            }
        } catch (Exception e) {
            log.warn("查询商家保证金记录失败, merchantId: {}", merchantId, e);
        }

        try {
            String flowSql = "SELECT * FROM " + paymentDatabase + ".deposit_flow WHERE merchant_id = ? ORDER BY created_at DESC LIMIT 20";
            List<Map<String, Object>> flowList = jdbcTemplate.queryForList(flowSql, merchantId);
            result.put("flows", flowList);
        } catch (Exception e) {
            log.warn("查询商家保证金流水失败, merchantId: {}", merchantId, e);
        }

        return result;
    }

    @Override
    public PageResponse<DepositFlow> flowPage(DepositFlowQueryRequest req) {
        LambdaQueryWrapper<DepositFlow> wrapper = new LambdaQueryWrapper<>();
        if (req.getMerchantId() != null) {
            wrapper.eq(DepositFlow::getMerchantId, req.getMerchantId());
        }
        if (req.getFlowType() != null) {
            wrapper.eq(DepositFlow::getFlowType, req.getFlowType());
        }
        if (req.getStartDate() != null) {
            wrapper.ge(DepositFlow::getCreateTime, req.getStartDate());
        }
        if (req.getEndDate() != null) {
            wrapper.le(DepositFlow::getCreateTime, req.getEndDate());
        }
        wrapper.orderByDesc(DepositFlow::getCreateTime);

        Page<DepositFlow> page = new Page<>(req.getPageNum(), req.getPageSize());
        depositFlowMapper.selectPage(page, wrapper);
        return PageResponse.of(page);
    }

    @Override
    public void adjustDeposit(DepositAdjustRequest req, Long adminId) {
        if (req.getAmount() == null || req.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("调整金额必须大于0");
        }
        if (req.getAdjustType() == null || req.getAdjustType() < 1 || req.getAdjustType() > 4) {
            throw new BusinessException("无效的调整类型");
        }

        Long qualityDepositId = getOrCreateQualityDeposit(req.getMerchantId());

        String selectSql = "SELECT * FROM " + paymentDatabase + ".quality_deposit WHERE id = ? FOR UPDATE";
        Map<String, Object> deposit;
        try {
            List<Map<String, Object>> list = jdbcTemplate.queryForList(selectSql, qualityDepositId);
            if (list.isEmpty()) {
                throw new BusinessException("保证金记录不存在");
            }
            deposit = list.get(0);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("查询保证金记录失败");
        }

        BigDecimal depositAmount = toBigDecimal(deposit.get("deposit_amount"));
        BigDecimal available = toBigDecimal(deposit.get("available_balance"));
        BigDecimal frozen = toBigDecimal(deposit.get("frozen_balance"));

        BigDecimal newDepositAmount = depositAmount;
        BigDecimal newAvailable = available;
        int flowType;
        String flowRemark;

        switch (req.getAdjustType()) {
            case 1:
            case 2:
                newDepositAmount = depositAmount.add(req.getAmount());
                newAvailable = available.add(req.getAmount());
                flowType = req.getAdjustType();
                flowRemark = req.getAdjustType() == 1 ? "管理员补缴保证金" : "管理员调增保证金";
                break;
            case 3:
                newAvailable = available.subtract(req.getAmount());
                if (newAvailable.compareTo(BigDecimal.ZERO) < 0) {
                    throw new BusinessException("调减后可用余额不能小于0");
                }
                flowType = 3;
                flowRemark = "管理员调减保证金";
                break;
            case 4:
                newDepositAmount = BigDecimal.ZERO;
                newAvailable = BigDecimal.ZERO;
                flowType = 4;
                flowRemark = "管理员退还保证金";
                break;
            default:
                throw new BusinessException("无效的调整类型");
        }

        String updateSql = "UPDATE " + paymentDatabase + ".quality_deposit SET deposit_amount = ?, available_balance = ?, updated_at = NOW() WHERE id = ?";
        jdbcTemplate.update(updateSql, newDepositAmount, newAvailable, qualityDepositId);

        DepositFlow flow = new DepositFlow();
        flow.setFlowNo(generateFlowNo(req.getMerchantId()));
        flow.setMerchantId(req.getMerchantId());
        flow.setQualityDepositId(qualityDepositId);
        flow.setFlowType(flowType);
        flow.setAmount(req.getAmount());
        flow.setBalanceBefore(available);
        flow.setBalanceAfter(newAvailable);
        flow.setRelatedType(5);
        flow.setOperatorId(adminId);
        flow.setOperatorType(AdminSecurityUtils.OPERATOR_TYPE_ADMIN);
        flow.setRemark((StringUtils.hasText(req.getRemark()) ? req.getRemark() + "，" : "") + flowRemark);
        if (StringUtils.hasText(req.getRelatedOrderNo())) {
            flow.setRelatedNo(req.getRelatedOrderNo());
        }
        depositFlowMapper.insert(flow);

        writeModifyLog(adminId, req.getMerchantId(), null, 8, "deposit",
                "deposit=" + depositAmount + "/available=" + available,
                "deposit=" + newDepositAmount + "/available=" + newAvailable,
                flowRemark + "，金额：" + req.getAmount() + "，备注：" + req.getRemark());

        log.info("保证金调整完成, merchantId: {}, adjustType: {}, amount: {}, adminId: {}",
                req.getMerchantId(), req.getAdjustType(), req.getAmount(), adminId);
    }

    private Long getOrCreateQualityDeposit(Long merchantId) {
        String sql = "SELECT id FROM " + paymentDatabase + ".quality_deposit WHERE merchant_id = ? LIMIT 1";
        try {
            List<Map<String, Object>> list = jdbcTemplate.queryForList(sql, merchantId);
            if (!list.isEmpty()) {
                return ((Number) list.get(0).get("id")).longValue();
            }
        } catch (Exception e) {
            log.warn("查询保证金记录失败", e);
        }

        String insertSql = "INSERT INTO " + paymentDatabase + ".quality_deposit (merchant_id, deposit_amount, available_balance, frozen_balance, status, created_at, updated_at) VALUES (?, 0, 0, 0, 1, NOW(), NOW())";
        jdbcTemplate.update(insertSql, merchantId);

        try {
            List<Map<String, Object>> list = jdbcTemplate.queryForList(sql, merchantId);
            if (!list.isEmpty()) {
                return ((Number) list.get(0).get("id")).longValue();
            }
        } catch (Exception e) {
            log.error("获取新创建的保证金记录失败", e);
        }
        throw new BusinessException("创建保证金记录失败");
    }

    private BigDecimal toBigDecimal(Object val) {
        if (val == null) return BigDecimal.ZERO;
        if (val instanceof BigDecimal) return (BigDecimal) val;
        try {
            return new BigDecimal(val.toString());
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private String generateFlowNo(Long merchantId) {
        return "DF" + System.currentTimeMillis() + (merchantId % 1000);
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
            modifyLogMapper.insert(logEntity);
        } catch (Exception e) {
            log.error("写入修改日志失败", e);
        }
    }
}
