package com.tailoris.api.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tailoris.api.product.entity.Product;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ProductMapper extends BaseMapper<Product> {
}
