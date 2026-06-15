package com.tailoris.product.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tailoris.product.dto.ProductCreateRequest;
import com.tailoris.product.dto.ProductQueryRequest;
import com.tailoris.product.dto.ProductUpdateRequest;
import com.tailoris.product.entity.Product;

public interface ProductService {

    Long createProduct(ProductCreateRequest request);

    void updateProduct(Long id, ProductUpdateRequest request);

    void deleteProduct(Long id);

    Product getProductDetail(Long id);

    Page<Product> listProducts(ProductQueryRequest request);

    Page<Product> listProductsByShop(Long shopId, ProductQueryRequest request);

    void updateProductStatus(Long id, Integer status);

    Page<Product> getProductByType(Integer productType, ProductQueryRequest request);

    void auditProduct(Long id, Integer auditStatus, String auditRemark, Long auditBy);
}
