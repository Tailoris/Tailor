package com.tailoris.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tailoris.order.entity.OrderInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Many;

@Mapper
public interface OrderInfoMapper extends BaseMapper<OrderInfo> {

    @Select("SELECT * FROM order_info WHERE order_no = #{orderNo}")
    @Results({
        @Result(column = "id", property = "id"),
        @Result(column = "id", property = "orderItems", 
                many = @Many(select = "com.tailoris.order.mapper.OrderItemMapper.selectByOrderId")),
        @Result(column = "id", property = "logistics", 
                one = @org.apache.ibatis.annotations.One(select = "com.tailoris.order.mapper.OrderLogisticsMapper.selectByOrderId"))
    })
    OrderInfo selectOrderDetailWithItems(@Param("orderNo") String orderNo);
}
