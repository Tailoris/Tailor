package com.tailoris.user.service;

import com.tailoris.user.entity.SysPermission;

import java.util.List;

public interface SysPermissionService {

    List<SysPermission> getPermissionsByUserId(Long userId);

    List<SysPermission> getPermissionsByRoleId(Long roleId);

    List<SysPermission> listAllPermissions();

    SysPermission getPermissionById(Long permissionId);

    SysPermission createPermission(SysPermission permission);

    void updatePermission(SysPermission permission);

    void deletePermission(Long permissionId);

    void assignPermissionToRole(Long roleId, Long permissionId);

    void removePermissionFromRole(Long roleId, Long permissionId);
}
