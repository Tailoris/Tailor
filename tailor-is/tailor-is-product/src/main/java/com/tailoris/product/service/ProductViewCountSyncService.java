package com.tailoris.product.service;

/**
 * 商品浏览量同步服务接口 - 修复 B-M33
 *
 * @author Tailor IS Team
 * @since 1.1.0
 */
public interface ProductViewCountSyncService {

    /**
     * 同步所有商品的浏览量
     *
     * @return 同步的记录数
     */
    int syncAllViewCounts();
}
