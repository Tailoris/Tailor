package com.tailoris.copyright.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tailoris.copyright.entity.CrViolationHandling;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CrViolationHandlingMapper extends BaseMapper<CrViolationHandling> {

    @Select("SELECT * FROM cr_violation_handling WHERE record_id = #{recordId} AND deleted = 0 ORDER BY create_time DESC")
    List<CrViolationHandling> selectByRecord(@Param("recordId") Long recordId);

    @Select("SELECT * FROM cr_violation_handling WHERE status = 0 AND deleted = 0 ORDER BY violation_level DESC, create_time ASC")
    List<CrViolationHandling> selectPending();
}
