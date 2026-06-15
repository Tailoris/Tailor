package com.tailoris.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tailoris.product.entity.CustomMeasurement;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CustomMeasurementMapper extends BaseMapper<CustomMeasurement> {

    /**
     * 根据订单ID查询定制参数.
     */
    CustomMeasurement selectByOrderId(@Param("orderId") Long orderId);
}
