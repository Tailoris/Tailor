package com.tailoris.merchant.service;

import com.tailoris.common.dto.PageResponse;
import com.tailoris.merchant.dto.MerchantApplyRequest;
import com.tailoris.merchant.dto.MerchantAuditRequest;
import com.tailoris.merchant.dto.MerchantQueryRequest;
import com.tailoris.merchant.entity.Merchant;

public interface MerchantService {

    void applyJoin(Long userId, MerchantApplyRequest request);

    void audit(MerchantAuditRequest request);

    Merchant getMerchantInfo(Long userId);

    Merchant getMerchantById(Long merchantId);

    PageResponse<Merchant> listMerchants(MerchantQueryRequest request);

    Merchant getMerchantByUserId(Long userId);
}
