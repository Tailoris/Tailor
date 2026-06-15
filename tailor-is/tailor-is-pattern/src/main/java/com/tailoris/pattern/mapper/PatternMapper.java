package com.tailoris.pattern.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tailoris.pattern.entity.Pattern;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PatternMapper extends BaseMapper<Pattern> {
}