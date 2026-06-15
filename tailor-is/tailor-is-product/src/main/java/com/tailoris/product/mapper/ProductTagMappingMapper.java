package com.tailoris.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tailoris.product.entity.ProductTagMapping;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;

@Mapper
public interface ProductTagMappingMapper extends BaseMapper<ProductTagMapping> {

    int insertBatchSomeColumn(Collection<ProductTagMapping> entityList);
}
