package com.tailoris.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tailoris.order.entity.ShoppingCart;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ShoppingCartMapper extends BaseMapper<ShoppingCart> {
}
