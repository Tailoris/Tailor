package com.tailoris.admin.service;

import com.tailoris.common.dto.PageResponse;

import java.util.Map;

public interface AdminMerchantBillService {

    PageResponse<Map<String, Object>> page(Long merchantId, String billType, String startDate, String endDate, Long current, Long size);
}
