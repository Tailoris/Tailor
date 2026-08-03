package com.tailoris.admin.service;

import com.tailoris.api.admin.dto.WithdrawAuditRequest;
import com.tailoris.common.dto.PageResponse;

import java.util.Map;

public interface AdminWithdrawAuditService {

    PageResponse<Map<String, Object>> page(String merchantName, Integer status, String startDate, String endDate, Long current, Long size);

    Map<String, Object> getDetail(Long withdrawId);

    /** @deprecated 请使用 audit(WithdrawAuditRequest, Long) 传递管理员ID */
    @Deprecated(forRemoval = true)
    default void audit(WithdrawAuditRequest req, String adminName) {
        audit(req, (Long) null);
    }

    void audit(WithdrawAuditRequest req, Long adminId);
}
