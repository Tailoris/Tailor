package com.tailoris.community.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tailoris.community.entity.CommunityReportAction;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CommunityReportActionMapper extends BaseMapper<CommunityReportAction> {

    @Select("SELECT * FROM community_report_action WHERE report_id = #{reportId} ORDER BY create_time DESC")
    List<CommunityReportAction> selectByReportId(@Param("reportId") Long reportId);
}
