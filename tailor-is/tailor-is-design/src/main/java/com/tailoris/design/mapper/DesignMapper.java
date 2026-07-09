package com.tailoris.design.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tailoris.design.entity.Design;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DesignMapper extends BaseMapper<Design> {
}