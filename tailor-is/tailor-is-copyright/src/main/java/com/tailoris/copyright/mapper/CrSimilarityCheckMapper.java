package com.tailoris.copyright.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tailoris.copyright.entity.CrSimilarityCheck;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CrSimilarityCheckMapper extends BaseMapper<CrSimilarityCheck> {

    @Select("SELECT * FROM cr_similarity_check WHERE source_record_id = #{id} AND is_infringement = 1 AND deleted = 0")
    List<CrSimilarityCheck> selectInfringementsBySource(@Param("id") Long id);

    @Select("SELECT * FROM cr_similarity_check WHERE target_record_id = #{id} AND is_infringement = 1 AND deleted = 0")
    List<CrSimilarityCheck> selectInfringementsByTarget(@Param("id") Long id);
}
