package com.tailoris.product.service;

import com.tailoris.product.entity.ProductSku;

import java.util.List;
import java.util.Map;

public interface ProductSkuService {

    Long createSku(Long productId, ProductSku sku);

    void updateSku(Long id, ProductSku sku);

    void deleteSku(Long id);

    List<ProductSku> listSkusByProduct(Long productId);

    boolean updateStock(Long skuId, Integer quantity, boolean increase);

    /**
     * 批量更新库存 - 修复 M-09: 避免逐条 UPDATE 引起的 N+1 问题
     *
     * @param stockChanges skuId -> quantity 的映射（quantity 为正数）
     * @param increase     true=增加库存，false=扣减库存
     * @return 成功更新的 SKU 数量
     */
    int batchUpdateStock(Map<Long, Integer> stockChanges, boolean increase);
}
