package com.tailoris.marketing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tailoris.marketing.entity.MktOrderPromotion;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface MktOrderPromotionMapper extends BaseMapper<MktOrderPromotion> {

    @Select("SELECT * FROM mkt_order_promotion WHERE order_id = #{orderId}")
    List<MktOrderPromotion> selectByOrderId(@Param("orderId") Long orderId);
}
