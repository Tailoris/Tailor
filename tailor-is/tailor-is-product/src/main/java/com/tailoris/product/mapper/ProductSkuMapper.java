package com.tailoris.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tailoris.product.entity.ProductSku;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;

@Mapper
public interface ProductSkuMapper extends BaseMapper<ProductSku> {

    int insertBatchSomeColumn(Collection<ProductSku> entityList);
}
