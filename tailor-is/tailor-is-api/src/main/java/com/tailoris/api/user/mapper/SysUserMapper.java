package com.tailoris.api.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tailoris.api.user.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {
}
