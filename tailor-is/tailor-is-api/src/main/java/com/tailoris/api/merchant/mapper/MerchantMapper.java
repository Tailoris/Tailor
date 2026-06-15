package com.tailoris.api.merchant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tailoris.api.merchant.entity.Merchant;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MerchantMapper extends BaseMapper<Merchant> {
}
