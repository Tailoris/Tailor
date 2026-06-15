package com.tailoris.product.service;

import com.tailoris.product.entity.ProductSku;

import java.util.List;

public interface ProductSkuService {

    Long createSku(Long productId, ProductSku sku);

    void updateSku(Long id, ProductSku sku);

    void deleteSku(Long id);

    List<ProductSku> listSkusByProduct(Long productId);

    boolean updateStock(Long skuId, Integer quantity, boolean increase);
}
