package com.tailoris.copyright.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tailoris.copyright.entity.CopyrightRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface CopyrightRecordMapper extends BaseMapper<CopyrightRecord> {

    @Select("SELECT * FROM copyright_record WHERE file_hash = #{fileHash}")
    CopyrightRecord selectByHash(String fileHash);

    @Select("SELECT * FROM copyright_record WHERE work_type = #{workType} AND status = 1 ORDER BY create_time DESC LIMIT #{limit}")
    java.util.List<CopyrightRecord> selectByWorkType(Integer workType, int limit);
}
