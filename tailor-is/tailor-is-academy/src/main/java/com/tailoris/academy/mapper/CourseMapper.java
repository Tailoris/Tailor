package com.tailoris.academy.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tailoris.academy.entity.Course;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CourseMapper extends BaseMapper<Course> {
}