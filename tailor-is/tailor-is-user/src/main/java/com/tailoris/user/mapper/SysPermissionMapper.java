package com.tailoris.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tailoris.user.entity.SysPermission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 系统权限 Mapper.
 *
 * <p>关键修复：</p>
 * <ul>
 *   <li>B-M15: 字符串拼接操作符置于行首</li>
 * </ul>
 *
 * @author Tailor IS Team
 * @since 1.0.0
 */
@Mapper
public interface SysPermissionMapper extends BaseMapper<SysPermission> {

    @Select("SELECT p.* FROM sys_permission p "
            + "INNER JOIN sys_role_permission rp ON p.id = rp.permission_id "
            + "INNER JOIN sys_user_role ur ON rp.role_id = ur.role_id "
            + "WHERE ur.user_id = #{userId} AND p.status = 1 AND p.deleted = 0")
    List<SysPermission> selectByUserId(@Param("userId") Long userId);
}
