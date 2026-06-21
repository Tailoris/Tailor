package com.tailoris.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tailoris.ai.entity.PatternRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface PatternRecordMapper extends BaseMapper<PatternRecord> {

    /**
     * 查询所有不重复的图案类型（修复 N+1：避免全量查询后逐记录查询）。
     *
     * @return 去重后的图案类型列表
     */
    @Select("SELECT DISTINCT pattern_type FROM pattern_record WHERE status = 1 AND pattern_type IS NOT NULL")
    List<Integer> selectDistinctPatternTypes();
}
