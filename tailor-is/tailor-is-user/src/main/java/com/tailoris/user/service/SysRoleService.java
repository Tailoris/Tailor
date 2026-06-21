package com.tailoris.user.service;

import com.tailoris.user.entity.SysRole;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface SysRoleService {

    List<SysRole> listRolesByUserId(Long userId);

    /**
     * 批量查询多个用户的角色（修复 N+1：分页场景下避免逐用户查询）.
     *
     * @param userIds 用户ID集合
     * @return userId -> 角色列表 的映射，无角色的用户对应空列表
     */
    Map<Long, List<SysRole>> listRolesByUserIds(Collection<Long> userIds);

    List<SysRole> listAllRoles();

    SysRole getRoleById(Long roleId);

    SysRole createRole(SysRole role);

    void updateRole(SysRole role);

    void deleteRole(Long roleId);

    void assignRoleToUser(Long userId, Long roleId);

    void removeRoleFromUser(Long userId, Long roleId);
}
