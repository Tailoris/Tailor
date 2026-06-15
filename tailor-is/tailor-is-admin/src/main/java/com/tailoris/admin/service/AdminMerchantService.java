package com.tailoris.admin.service;

import com.tailoris.api.admin.dto.MerchantAuditRequest;
import com.tailoris.api.admin.dto.MerchantQueryRequest;
import com.tailoris.common.dto.PageResponse;
import com.tailoris.api.merchant.entity.Merchant;

import java.util.List;

public interface AdminMerchantService {

    PageResponse<Merchant> listPendingMerchants(MerchantQueryRequest request);

    void auditMerchant(MerchantAuditRequest request, Long adminId);

    void freezeMerchant(Long merchantId);

    void unfreezeMerchant(Long merchantId);

    PageResponse<Merchant> listMerchants(MerchantQueryRequest request);

    Merchant getMerchantDetail(Long merchantId);
}
