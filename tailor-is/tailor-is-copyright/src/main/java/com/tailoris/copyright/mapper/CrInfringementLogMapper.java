package com.tailoris.copyright.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tailoris.copyright.entity.CrInfringementLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CrInfringementLogMapper extends BaseMapper<CrInfringementLog> {

    @Select("SELECT * FROM cr_infringement_log WHERE case_id = #{caseId} AND deleted = 0 ORDER BY create_time ASC")
    List<CrInfringementLog> selectByCase(@Param("caseId") Long caseId);
}
