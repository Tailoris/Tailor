package com.tailoris.admin.service;

import com.tailoris.api.admin.dto.MerchantModifyApplyAuditRequest;
import com.tailoris.api.admin.dto.MerchantModifyApplyQueryRequest;
import com.tailoris.common.dto.PageResponse;
import com.tailoris.api.merchant.entity.MerchantModifyApply;

public interface AdminMerchantModifyService {

    PageResponse<MerchantModifyApply> page(MerchantModifyApplyQueryRequest req);

    MerchantModifyApply detail(Long applyId);

    /** @deprecated 请使用 audit(MerchantModifyApplyAuditRequest, Long) 传递管理员ID */
    @Deprecated(forRemoval = true)
    default void audit(MerchantModifyApplyAuditRequest req, String adminName) {
        audit(req, (Long) null);
    }

    void audit(MerchantModifyApplyAuditRequest req, Long adminId);
}
