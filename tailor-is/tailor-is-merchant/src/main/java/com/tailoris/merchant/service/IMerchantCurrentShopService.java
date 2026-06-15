package com.tailoris.merchant.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tailoris.merchant.entity.MerchantCurrentShop;
import com.tailoris.merchant.entity.MerchantShop;

import java.util.List;

/**
 * 商家多店铺切换服务接口 - MER-003.
 *
 * <p>支持用户在同一商家下快速切换不同店铺的工作上下文。</p>
 *
 * @author Tailor IS Team
 * @since 2026-06-03
 */
public interface IMerchantCurrentShopService extends IService<MerchantCurrentShop> {

    /**
     * 获取用户当前操作的店铺ID.
     */
    Long getCurrentShopId(Long userId, Long merchantId);

    /**
     * 切换到指定店铺.
     */
    boolean switchTo(Long userId, Long merchantId, Long targetShopId);

    /**
     * 获取用户在某商家下可见的店铺列表.
     */
    List<MerchantShop> listUserShops(Long userId, Long merchantId);

    /**
     * 清除当前店铺记录（登出时调用）.
     */
    boolean clear(Long userId, Long merchantId);
}
