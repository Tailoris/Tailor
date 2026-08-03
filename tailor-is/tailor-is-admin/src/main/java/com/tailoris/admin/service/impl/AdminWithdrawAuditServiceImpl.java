package com.tailoris.admin.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tailoris.api.admin.dto.WithdrawAuditRequest;
import com.tailoris.admin.service.AdminWithdrawAuditService;
import com.tailoris.common.dto.PageResponse;
import com.tailoris.common.exception.BusinessException;
import com.tailoris.api.merchant.entity.MerchantModifyLog;
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
public class AdminWithdrawAuditServiceImpl implements AdminWithdrawAuditService {

    private final JdbcTemplate jdbcTemplate;
    private final MerchantModifyLogMapper modifyLogMapper;
    private final ObjectMapper objectMapper;
    private final HttpServletRequest request;

    @Value("${tailoris.datasource.merchant-db:tailor_is_merchant}")
    private String merchantDatabase;

    @Value("${tailoris.datasource.payment-db:tailor_is_payment}")
    private String paymentDatabase;

    @Override
    public PageResponse<Map<String, Object>> page(String merchantName, Integer status, String startDate, String endDate, Long current, Long size) {
        int pageNum = current != null ? current.intValue() : 1;
        int pageSize = size != null ? size.intValue() : 10;
        int offset = (pageNum - 1) * pageSize;

        StringBuilder sql = new StringBuilder("SELECT wr.*, m.company_name AS merchant_name, m.contact_name ")
                .append("FROM ").append(paymentDatabase).append(".withdraw_record wr ")
                .append("LEFT JOIN ").append(merchantDatabase).append(".merchant m ON wr.merchant_id = m.id ")
                .append("WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (StringUtils.hasText(merchantName)) {
            sql.append(" AND m.company_name LIKE ?");
            params.add("%" + merchantName + "%");
        }
        if (status != null) {
            sql.append(" AND wr.status = ?");
            params.add(status);
        }
        if (StringUtils.hasText(startDate)) {
            sql.append(" AND wr.created_at >= ?");
            params.add(startDate);
        }
        if (StringUtils.hasText(endDate)) {
            sql.append(" AND wr.created_at <= ?");
            params.add(endDate);
        }

        String countSql = "SELECT COUNT(*) FROM (" + sql + ") t";
        Long total;
        try {
            total = jdbcTemplate.queryForObject(countSql, Long.class, params.toArray());
        } catch (Exception e) {
            total = 0L;
        }
        if (total == null) total = 0L;

        sql.append(" ORDER BY wr.created_at DESC LIMIT ? OFFSET ?");
        params.add(pageSize);
        params.add(offset);

        List<Map<String, Object>> records = jdbcTemplate.queryForList(sql.toString(), params.toArray());
        return PageResponse.of(records, total, pageNum, pageSize);
    }

    @Override
    public Map<String, Object> getDetail(Long withdrawId) {
        String sql = "SELECT wr.*, m.company_name AS merchant_name, m.contact_name, m.contact_phone " +
                "FROM " + paymentDatabase + ".withdraw_record wr " +
                "LEFT JOIN " + merchantDatabase + ".merchant m ON wr.merchant_id = m.id " +
                "WHERE wr.id = ?";
        List<Map<String, Object>> list = jdbcTemplate.queryForList(sql, withdrawId);
        if (list.isEmpty()) {
            throw new BusinessException("提现记录不存在");
        }
        return list.get(0);
    }

    @Override
    public void audit(WithdrawAuditRequest req, Long adminId) {
        Map<String, Object> detail = getDetail(req.getWithdrawId());
        Integer currentStatus = detail.get("status") != null
                ? ((Number) detail.get("status")).intValue() : null;

        if (currentStatus == null || currentStatus != 1) {
            throw new BusinessException("该提现申请状态不允许审核（当前状态非待审核）");
        }

        Long merchantId = detail.get("merchant_id") != null
                ? ((Number) detail.get("merchant_id")).longValue() : null;

        if (Boolean.TRUE.equals(req.getApproved())) {
            String updateSql = "UPDATE " + paymentDatabase + ".withdraw_record SET status = 2, audit_by = ?, audit_time = ?, audit_remark = ? WHERE id = ?";
            jdbcTemplate.update(updateSql, adminId, LocalDateTime.now(), req.getRemark(), req.getWithdrawId());

            writeModifyLog(adminId, merchantId, null, 9, "withdraw",
                    "status=1", "status=2",
                    "提现审核通过: " + req.getWithdrawId() + (StringUtils.hasText(req.getRemark()) ? "，备注：" + req.getRemark() : ""));
            log.info("提现审核通过, withdrawId: {}, merchantId: {}, adminId: {}", req.getWithdrawId(), merchantId, adminId);
        } else {
            if (!StringUtils.hasText(req.getRemark())) {
                throw new BusinessException("驳回时必须填写原因");
            }
            String updateSql = "UPDATE " + paymentDatabase + ".withdraw_record SET status = 4, audit_by = ?, audit_time = ?, fail_reason = ? WHERE id = ?";
            jdbcTemplate.update(updateSql, adminId, LocalDateTime.now(), req.getRemark(), req.getWithdrawId());

            writeModifyLog(adminId, merchantId, null, 9, "withdraw",
                    "status=1", "status=4",
                    "提现审核驳回: " + req.getWithdrawId() + "，原因：" + req.getRemark());
            log.info("提现审核驳回, withdrawId: {}, merchantId: {}, adminId: {}", req.getWithdrawId(), merchantId, adminId);
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
            modifyLogMapper.insert(logEntity);
        } catch (Exception e) {
            log.error("写入修改日志失败", e);
        }
    }
}
