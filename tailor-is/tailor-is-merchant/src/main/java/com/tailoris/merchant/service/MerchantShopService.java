package com.tailoris.merchant.service;

import com.tailoris.merchant.dto.ShopDecorationRequest;
import com.tailoris.merchant.dto.ShopUpdateRequest;
import com.tailoris.merchant.entity.MerchantShop;

import java.util.List;
import java.util.Map;

/**
 * 商户店铺服务接口.
 *
 * @author Tailor IS Team
 * @since 2026-06-03
 */
public interface MerchantShopService {

    MerchantShop createShop(Long merchantId, ShopUpdateRequest request);

    void updateShop(Long merchantId, Long shopId, ShopUpdateRequest request);

    void updateShopStatus(Long merchantId, Long shopId, Integer shopStatus);

    MerchantShop getShopInfo(Long shopId);

    List<MerchantShop> listShopsByMerchant(Long merchantId);

    /**
     * 保存店铺装修配置 - MER-004.
     */
    MerchantShop saveDecoration(Long merchantId, ShopDecorationRequest request);

    /**
     * 获取店铺装修配置（含解析）.
     */
    Map<String, Object> getDecoration(Long shopId);

    /**
     * 预览装修效果.
     */
    Map<String, Object> previewDecoration(Long shopId);
}
