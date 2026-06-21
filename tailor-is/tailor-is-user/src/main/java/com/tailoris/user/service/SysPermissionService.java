package com.tailoris.user.service;

import com.tailoris.user.entity.SysPermission;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface SysPermissionService {

    List<SysPermission> getPermissionsByUserId(Long userId);

    /**
     * 批量查询多个用户的权限（修复 N+1：分页场景下避免逐用户查询）.
     *
     * @param userIds 用户ID集合
     * @return userId -> 权限列表 的映射，无权限的用户对应空列表
     */
    Map<Long, List<SysPermission>> getPermissionsByUserIds(Collection<Long> userIds);

    List<SysPermission> getPermissionsByRoleId(Long roleId);

    List<SysPermission> listAllPermissions();

    SysPermission getPermissionById(Long permissionId);

    SysPermission createPermission(SysPermission permission);

    void updatePermission(SysPermission permission);

    void deletePermission(Long permissionId);

    void assignPermissionToRole(Long roleId, Long permissionId);

    void removePermissionFromRole(Long roleId, Long permissionId);
}
