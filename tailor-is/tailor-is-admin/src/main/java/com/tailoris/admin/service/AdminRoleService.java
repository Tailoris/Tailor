package com.tailoris.admin.service;

import com.tailoris.common.dto.PageResponse;

import java.util.List;
import java.util.Map;

public interface AdminRoleService {

    PageResponse<Map<String, Object>> listRoles(String keyword, Integer current, Integer size);

    Map<String, Object> getRoleDetail(Long id);

    Map<String, Object> createRole(Map<String, Object> role);

    Map<String, Object> updateRole(Long id, Map<String, Object> role);

    void deleteRole(Long id);

    List<Map<String, Object>> getRolePermissions(Long roleId);

    void assignRolePermissions(Long roleId, Long[] permissionIds);

    PageResponse<Map<String, Object>> getRoleUsers(Long roleId, Integer current, Integer size);

    List<Map<String, Object>> listAllRoles();
}