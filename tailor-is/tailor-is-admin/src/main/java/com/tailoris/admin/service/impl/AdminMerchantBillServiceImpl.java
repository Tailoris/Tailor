package com.tailoris.admin.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tailoris.admin.service.AdminMerchantBillService;
import com.tailoris.common.dto.PageResponse;
import com.tailoris.common.exception.BusinessException;
import com.tailoris.api.merchant.entity.MerchantModifyLog;
import com.tailoris.api.merchant.mapper.MerchantModifyLogMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminMerchantBillServiceImpl implements AdminMerchantBillService {

    private static final Set<String> VALID_BILL_TYPES = new HashSet<>(Arrays.asList(
            "settlement", "withdraw", "deposit_flow", "transaction"
    ));

    private final JdbcTemplate jdbcTemplate;
    private final MerchantModifyLogMapper modifyLogMapper;
    private final ObjectMapper objectMapper;
    private final HttpServletRequest request;

    @Value("${tailoris.datasource.merchant-db:tailor_is_merchant}")
    private String merchantDatabase;

    @Value("${tailoris.datasource.payment-db:tailor_is_payment}")
    private String paymentDatabase;

    @Override
    public PageResponse<Map<String, Object>> page(Long merchantId, String billType, String startDate, String endDate, Long current, Long size) {
        if (StringUtils.hasText(billType) && !VALID_BILL_TYPES.contains(billType)) {
            throw new BusinessException("无效的账单类型，可选值：settlement, withdraw, deposit_flow, transaction");
        }

        int pageNum = current != null ? current.intValue() : 1;
        int pageSize = size != null ? size.intValue() : 10;
        int offset = (pageNum - 1) * pageSize;

        if (!StringUtils.hasText(billType)) {
            return unionAllBills(merchantId, startDate, endDate, pageNum, pageSize, offset);
        }

        return singleTypeBills(merchantId, billType, startDate, endDate, pageNum, pageSize, offset);
    }

    private PageResponse<Map<String, Object>> unionAllBills(Long merchantId, String startDate, String endDate,
                                                            int pageNum, int pageSize, int offset) {
        StringBuilder unionSql = new StringBuilder();
        List<Object> allParams = new ArrayList<>();

        String[] tables = {
                "settlement_record", "withdraw_record", "deposit_flow", "account_transaction"
        };
        String[] billTypeLabels = {"settlement", "withdraw", "deposit_flow", "transaction"};
        String[] idColumns = {"settlement_no", "withdraw_no", "flow_no", "trans_no"};
        String[] amountColumns = {"settlement_amount", "amount", "amount", "amount"};
        String[] statusColumns = {"status", "status", "flow_type", "status"};
        String[] timeColumns = {"settlement_time", "created_at", "created_at", "trans_time"};

        boolean first = true;
        for (int i = 0; i < tables.length; i++) {
            if (!first) {
                unionSql.append(" UNION ALL ");
            }
            unionSql.append("SELECT '")
                    .append(billTypeLabels[i]).append("' AS bill_type, ")
                    .append(idColumns[i]).append(" AS bill_no, ")
                    .append(amountColumns[i]).append(" AS amount, ")
                    .append(statusColumns[i]).append(" AS status, ")
                    .append(timeColumns[i]).append(" AS bill_time, ")
                    .append("merchant_id, created_at AS created_time ")
                    .append("FROM ").append(paymentDatabase).append(".").append(tables[i])
                    .append(" WHERE ");
            if ("account_transaction".equals(tables[i])) {
                unionSql.append("account_type = 2 AND ");
            }
            unionSql.append("1=1");
            if (merchantId != null) {
                unionSql.append(" AND merchant_id = ?");
                allParams.add(merchantId);
            }
            if (StringUtils.hasText(startDate)) {
                unionSql.append(" AND ").append(timeColumns[i]).append(" >= ?");
                allParams.add(startDate);
            }
            if (StringUtils.hasText(endDate)) {
                unionSql.append(" AND ").append(timeColumns[i]).append(" <= ?");
                allParams.add(endDate);
            }
            first = false;
        }

        String countSql = "SELECT COUNT(*) FROM (" + unionSql + ") t";
        Long total = 0L;
        try {
            total = jdbcTemplate.queryForObject(countSql, Long.class, allParams.toArray());
        } catch (Exception e) {
            log.warn("查询账单总数失败", e);
        }
        if (total == null) total = 0L;

        unionSql.append(" ORDER BY bill_time DESC LIMIT ? OFFSET ?");
        allParams.add(pageSize);
        allParams.add(offset);

        List<Map<String, Object>> records;
        try {
            records = jdbcTemplate.queryForList(unionSql.toString(), allParams.toArray());
        } catch (Exception e) {
            log.warn("联合查询账单失败，回退为空列表", e);
            records = new ArrayList<>();
        }

        return PageResponse.of(records, total, pageNum, pageSize);
    }

    private PageResponse<Map<String, Object>> singleTypeBills(Long merchantId, String billType, String startDate, String endDate,
                                                              int pageNum, int pageSize, int offset) {
        String table;
        String idColumn;
        String amountColumn;
        String timeColumn;
        switch (billType) {
            case "settlement":
                table = "settlement_record";
                idColumn = "settlement_no";
                amountColumn = "settlement_amount";
                timeColumn = "settlement_time";
                break;
            case "withdraw":
                table = "withdraw_record";
                idColumn = "withdraw_no";
                amountColumn = "amount";
                timeColumn = "created_at";
                break;
            case "deposit_flow":
                table = "deposit_flow";
                idColumn = "flow_no";
                amountColumn = "amount";
                timeColumn = "created_at";
                break;
            case "transaction":
                table = "account_transaction";
                idColumn = "trans_no";
                amountColumn = "amount";
                timeColumn = "trans_time";
                break;
            default:
                return PageResponse.empty(pageNum, pageSize);
        }

        StringBuilder sql = new StringBuilder("SELECT '")
                .append(billType).append("' AS bill_type, t.* ")
                .append("FROM ").append(paymentDatabase).append(".").append(table).append(" t WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (merchantId != null) {
            sql.append(" AND t.merchant_id = ?");
            params.add(merchantId);
        }
        if ("transaction".equals(billType)) {
            sql.append(" AND t.account_type = 2");
        }
        if (StringUtils.hasText(startDate)) {
            sql.append(" AND t.").append(timeColumn).append(" >= ?");
            params.add(startDate);
        }
        if (StringUtils.hasText(endDate)) {
            sql.append(" AND t.").append(timeColumn).append(" <= ?");
            params.add(endDate);
        }

        String countSql = "SELECT COUNT(*) FROM (" + sql + ") t";
        Long total = 0L;
        try {
            total = jdbcTemplate.queryForObject(countSql, Long.class, params.toArray());
        } catch (Exception e) {
            log.warn("查询单类型账单总数失败", e);
        }
        if (total == null) total = 0L;

        sql.append(" ORDER BY t.").append(timeColumn).append(" DESC LIMIT ? OFFSET ?");
        params.add(pageSize);
        params.add(offset);

        List<Map<String, Object>> records;
        try {
            records = jdbcTemplate.queryForList(sql.toString(), params.toArray());
        } catch (Exception e) {
            log.warn("查询单类型账单失败", e);
            records = new ArrayList<>();
        }

        return PageResponse.of(records, total, pageNum, pageSize);
    }
}
