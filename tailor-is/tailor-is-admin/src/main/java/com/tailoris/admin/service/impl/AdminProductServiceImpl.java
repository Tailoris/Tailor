package com.tailoris.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tailoris.admin.constant.AdminConstants;
import com.tailoris.api.admin.dto.ProductAuditRequest;
import com.tailoris.admin.service.AdminProductService;
import com.tailoris.common.dto.PageResponse;
import com.tailoris.common.dto.PageRequest;
import com.tailoris.common.exception.BusinessException;
import com.tailoris.api.product.entity.Product;
import com.tailoris.api.product.mapper.ProductMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminProductServiceImpl implements AdminProductService {

    private final ProductMapper productMapper;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public PageResponse<Product> listPendingProducts(PageRequest request) {
        LambdaQueryWrapper<Product> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Product::getAuditStatus, AdminConstants.AUDIT_STATUS_PENDING);
        queryWrapper.orderByDesc(Product::getCreateTime);

        Page<Product> page = new Page<>(request.getPageNum(), request.getPageSize());
        Page<Product> result = productMapper.selectPage(page, queryWrapper);

        return new PageResponse<>(result.getRecords(), result.getTotal(), request.getPageNum(), request.getPageSize());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditProduct(ProductAuditRequest request, Long adminId) {
        Product product = productMapper.selectById(request.getProductId());
        if (product == null) {
            throw new BusinessException("商品不存在");
        }

        if (product.getAuditStatus().equals(AdminConstants.AUDIT_STATUS_APPROVED)) {
            throw new BusinessException("商品已通过审核，无需重复操作");
        }

        Product updateProduct = new Product();
        updateProduct.setId(request.getProductId());
        updateProduct.setAuditStatus(request.getAuditStatus());
        updateProduct.setAuditRemark(request.getAuditRemark());
        updateProduct.setAuditBy(adminId);
        updateProduct.setAuditTime(LocalDateTime.now());

        if (request.getAuditStatus().equals(AdminConstants.AUDIT_STATUS_APPROVED)) {
            updateProduct.setStatus(2);
        } else if (request.getAuditStatus().equals(AdminConstants.AUDIT_STATUS_REJECTED)) {
            updateProduct.setStatus(4);
        }

        productMapper.updateById(updateProduct);

        stringRedisTemplate.delete("product:detail:" + request.getProductId());

        log.info("商品审核完成, productId: {}, auditStatus: {}", request.getProductId(), request.getAuditStatus());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectProduct(Long productId, String remark, Long adminId) {
        Product product = productMapper.selectById(productId);
        if (product == null) {
            throw new BusinessException("商品不存在");
        }

        Product updateProduct = new Product();
        updateProduct.setId(productId);
        updateProduct.setAuditStatus(AdminConstants.AUDIT_STATUS_REJECTED);
        updateProduct.setStatus(4);
        updateProduct.setAuditRemark(remark);
        updateProduct.setAuditBy(adminId);
        updateProduct.setAuditTime(LocalDateTime.now());

        productMapper.updateById(updateProduct);
        stringRedisTemplate.delete("product:detail:" + productId);

        log.info("商品被驳回, productId: {}, remark: {}", productId, remark);
    }
}
