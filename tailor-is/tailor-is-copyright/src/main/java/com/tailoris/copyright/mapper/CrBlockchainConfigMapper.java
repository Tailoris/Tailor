package com.tailoris.copyright.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tailoris.copyright.entity.CrBlockchainConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CrBlockchainConfigMapper extends BaseMapper<CrBlockchainConfig> {

    @Select("SELECT * FROM cr_blockchain_config WHERE is_active = 1 AND is_default = 1 AND deleted = 0 LIMIT 1")
    CrBlockchainConfig selectDefault();

    @Select("SELECT * FROM cr_blockchain_config WHERE is_active = 1 AND deleted = 0 ORDER BY priority DESC")
    List<CrBlockchainConfig> selectAllActive();
}
