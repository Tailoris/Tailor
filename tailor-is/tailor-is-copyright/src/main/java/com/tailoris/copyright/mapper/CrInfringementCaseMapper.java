package com.tailoris.copyright.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tailoris.copyright.entity.CrInfringementCase;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface CrInfringementCaseMapper extends BaseMapper<CrInfringementCase> {

    @Select("SELECT * FROM cr_infringement_case WHERE arbitration_deadline < NOW() AND status IN (0,1,2) AND deleted = 0")
    List<CrInfringementCase> selectOverdueArbitration();

    @Select("SELECT * FROM cr_infringement_case WHERE copyright_user_id = #{userId} AND deleted = 0 ORDER BY create_time DESC")
    List<CrInfringementCase> selectByUser(@Param("userId") Long userId);

    @Update("UPDATE cr_infringement_case SET status = #{toStatus}, update_time = NOW() WHERE id = #{id} AND status = #{fromStatus}")
    int updateStatusIfMatch(@Param("id") Long id, @Param("fromStatus") Integer fromStatus, @Param("toStatus") Integer toStatus);
}
