package com.tailoris.copyright.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tailoris.copyright.entity.CrInspectionTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CrInspectionTaskMapper extends BaseMapper<CrInspectionTask> {

    @Select("SELECT * FROM cr_inspection_task WHERE status = 0 AND scheduled_time < NOW() AND deleted = 0 ORDER BY scheduled_time ASC LIMIT 50")
    List<CrInspectionTask> selectDue();

    @Select("SELECT * FROM cr_inspection_task WHERE task_type = #{type} AND status = 2 AND create_time BETWEEN #{start} AND #{end} AND deleted = 0 ORDER BY create_time DESC")
    List<CrInspectionTask> selectByTypeAndTime(@Param("type") Integer type, @Param("start") String start, @Param("end") String end);
}
