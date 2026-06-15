package com.tailoris.marketing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tailoris.marketing.entity.MktSkuPromotionPrice;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface MktSkuPromotionPriceMapper extends BaseMapper<MktSkuPromotionPrice> {

    @Select("SELECT * FROM mkt_sku_promotion_price WHERE sku_id = #{skuId} AND (NOW() BETWEEN start_time AND end_time) ORDER BY promotion_price ASC LIMIT 1")
    MktSkuPromotionPrice selectActiveBySkuId(@Param("skuId") Long skuId);

    @Select("SELECT * FROM mkt_sku_promotion_price WHERE product_id = #{productId} AND (NOW() BETWEEN start_time AND end_time) ORDER BY promotion_price ASC")
    List<MktSkuPromotionPrice> selectActiveByProductId(@Param("productId") Long productId);
}
