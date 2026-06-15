package com.tailoris.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tailoris.order.entity.OrderLogistics;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface OrderLogisticsMapper extends BaseMapper<OrderLogistics> {

    @Select("SELECT * FROM order_logistics WHERE order_id = #{orderId}")
    OrderLogistics selectByOrderId(@Param("orderId") Long orderId);
}
