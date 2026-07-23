package com.tailoris.admin.service.impl;

import com.tailoris.admin.constant.AdminConstants;
import com.tailoris.api.admin.dto.ProductAuditRequest;
import com.tailoris.admin.feign.ProductFeignClient;
import com.tailoris.admin.service.AdminProductService;
import com.tailoris.common.dto.PageResponse;
import com.tailoris.common.dto.PageRequest;
import com.tailoris.common.exception.BusinessException;
import com.tailoris.common.result.Result;
import com.tailoris.api.product.entity.Product;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminProductServiceImpl implements AdminProductService {

    private final ProductFeignClient productFeignClient;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public PageResponse<Product> listPendingProducts(PageRequest request) {
        Map<String, Object> params = new HashMap<>();
        params.put("pageNum", request.getPageNum());
        params.put("pageSize", request.getPageSize());
        params.put("auditStatus", AdminConstants.AUDIT_STATUS_PENDING);

        Result<PageResponse<Product>> result = productFeignClient.listProducts(params);
        if (!result.isSuccess()) {
            throw new BusinessException(result.getMessage());
        }
        return result.getData();
    }

    @Override
    public void auditProduct(ProductAuditRequest request, Long adminId) {
        Result<Product> result = productFeignClient.getProductDetail(request.getProductId());
        if (!result.isSuccess() || result.getData() == null) {
            throw new BusinessException("商品不存在");
        }

        Product product = result.getData();
        if (product.getAuditStatus().equals(AdminConstants.AUDIT_STATUS_APPROVED)) {
            throw new BusinessException("商品已通过审核，无需重复操作");
        }

        int status;
        if (request.getAuditStatus().equals(AdminConstants.AUDIT_STATUS_APPROVED)) {
            status = 2;
        } else if (request.getAuditStatus().equals(AdminConstants.AUDIT_STATUS_REJECTED)) {
            status = 4;
        } else {
            status = product.getStatus();
        }

        Result<Void> updateResult = productFeignClient.updateProductStatus(request.getProductId(), status);
        if (!updateResult.isSuccess()) {
            throw new BusinessException(updateResult.getMessage());
        }

        stringRedisTemplate.delete("product:detail:" + request.getProductId());

        log.info("商品审核完成, productId: {}, auditStatus: {}", request.getProductId(), request.getAuditStatus());
    }

    @Override
    public void rejectProduct(Long productId, String remark, Long adminId) {
        Result<Product> result = productFeignClient.getProductDetail(productId);
        if (!result.isSuccess() || result.getData() == null) {
            throw new BusinessException("商品不存在");
        }

        Result<Void> updateResult = productFeignClient.updateProductStatus(productId, 4);
        if (!updateResult.isSuccess()) {
            throw new BusinessException(updateResult.getMessage());
        }

        stringRedisTemplate.delete("product:detail:" + productId);

        log.info("商品被驳回, productId: {}, remark: {}", productId, remark);
    }
}