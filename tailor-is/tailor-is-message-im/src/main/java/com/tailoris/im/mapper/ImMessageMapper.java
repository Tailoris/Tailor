package com.tailoris.im.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tailoris.im.entity.ImMessage;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ImMessageMapper extends BaseMapper<ImMessage> {
}