package com.tailoris.copyright.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tailoris.copyright.entity.CrIpBlacklist;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CrIpBlacklistMapper extends BaseMapper<CrIpBlacklist> {

    @Select("SELECT * FROM cr_ip_blacklist WHERE list_type = #{listType} AND target_type = #{targetType} " +
            "AND target_value = #{value} AND (expire_time IS NULL OR expire_time > NOW()) AND deleted = 0 LIMIT 1")
    CrIpBlacklist selectMatch(@Param("listType") Integer listType,
                              @Param("targetType") Integer targetType,
                              @Param("value") String value);

    @Select("SELECT * FROM cr_ip_blacklist WHERE list_type = 1 AND deleted = 0 ORDER BY create_time DESC LIMIT 1000")
    List<CrIpBlacklist> selectAllBlack();
}
