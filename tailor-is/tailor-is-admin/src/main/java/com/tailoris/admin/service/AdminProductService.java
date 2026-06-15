package com.tailoris.admin.service;

import com.tailoris.api.admin.dto.ProductAuditRequest;
import com.tailoris.common.dto.PageResponse;
import com.tailoris.common.dto.PageRequest;
import com.tailoris.api.product.entity.Product;

public interface AdminProductService {

    PageResponse<Product> listPendingProducts(PageRequest request);

    void auditProduct(ProductAuditRequest request, Long adminId);

    void rejectProduct(Long productId, String remark, Long adminId);
}
