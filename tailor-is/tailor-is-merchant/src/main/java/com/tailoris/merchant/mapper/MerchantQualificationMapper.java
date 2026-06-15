package com.tailoris.merchant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tailoris.merchant.entity.MerchantQualification;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;

@Mapper
public interface MerchantQualificationMapper extends BaseMapper<MerchantQualification> {

    int insertBatchSomeColumn(Collection<MerchantQualification> entityList);
}
