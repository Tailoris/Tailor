package com.tailoris.admin.service;

import com.tailoris.api.admin.dto.DepositAdjustRequest;
import com.tailoris.api.admin.dto.DepositFlowQueryRequest;
import com.tailoris.api.admin.dto.DepositQueryRequest;
import com.tailoris.api.merchant.entity.DepositFlow;
import com.tailoris.common.dto.PageResponse;

import java.util.Map;

public interface AdminDepositService {

    PageResponse<Map<String, Object>> page(DepositQueryRequest req);

    Map<String, Object> getMerchantDepositDetail(Long merchantId);

    PageResponse<DepositFlow> flowPage(DepositFlowQueryRequest req);

    /** @deprecated 请使用 adjustDeposit(DepositAdjustRequest, Long) 传递管理员ID */
    @Deprecated(forRemoval = true)
    default void adjustDeposit(DepositAdjustRequest req, String adminName) {
        adjustDeposit(req, (Long) null);
    }

    void adjustDeposit(DepositAdjustRequest req, Long adminId);
}
