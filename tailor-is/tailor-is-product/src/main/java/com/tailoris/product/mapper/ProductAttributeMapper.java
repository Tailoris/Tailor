package com.tailoris.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tailoris.product.entity.ProductAttribute;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;

@Mapper
public interface ProductAttributeMapper extends BaseMapper<ProductAttribute> {

    int insertBatchSomeColumn(Collection<ProductAttribute> entityList);
}
