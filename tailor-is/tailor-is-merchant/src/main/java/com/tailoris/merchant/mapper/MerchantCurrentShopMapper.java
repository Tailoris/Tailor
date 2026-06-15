package com.tailoris.merchant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tailoris.merchant.entity.MerchantCurrentShop;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 商家当前操作店铺Mapper - MER-003.
 *
 * @author Tailor IS Team
 * @since 2026-06-03
 */
@Mapper
public interface MerchantCurrentShopMapper extends BaseMapper<MerchantCurrentShop> {

    @Select("SELECT * FROM merchant_current_shop " +
            "WHERE user_id = #{userId} AND merchant_id = #{merchantId} LIMIT 1")
    MerchantCurrentShop selectByUserAndMerchant(
            @Param("userId") Long userId,
            @Param("merchantId") Long merchantId);
}
