package com.tailoris.user.service;

import com.tailoris.user.entity.SysRole;

import java.util.List;

public interface SysRoleService {

    List<SysRole> listRolesByUserId(Long userId);

    List<SysRole> listAllRoles();

    SysRole getRoleById(Long roleId);

    SysRole createRole(SysRole role);

    void updateRole(SysRole role);

    void deleteRole(Long roleId);

    void assignRoleToUser(Long userId, Long roleId);

    void removeRoleFromUser(Long userId, Long roleId);
}
