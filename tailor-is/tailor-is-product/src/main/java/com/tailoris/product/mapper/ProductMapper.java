package com.tailoris.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tailoris.product.entity.Product;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ProductMapper extends BaseMapper<Product> {
}
