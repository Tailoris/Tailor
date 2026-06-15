package com.tailoris.merchant.service;

import java.util.List;
import java.util.Set;

/**
 * 商家权限服务接口（按钮级） - MER-002.
 *
 * <p>提供商家员工细粒度权限校验能力。</p>
 *
 * <h3>权限编码格式</h3>
 * <p>采用 <code>模块:动作</code> 格式，例如：<code>product:create</code></p>
 *
 * @author Tailor IS Team
 * @since 2026-06-03
 */
public interface IMerchantPermissionService {

    /**
     * 检查员工是否拥有指定权限.
     */
    boolean hasPermission(Long employeeId, String permissionCode);

    /**
     * 获取员工的所有权限编码.
     */
    Set<String> getEmployeePermissions(Long employeeId);

    /**
     * 批量校验权限.
     */
    boolean[] hasPermissions(Long employeeId, String[] permissionCodes);

    /**
     * 按角色代码获取默认权限.
     */
    List<String> getDefaultPermissionsByRoleCode(String roleCode);

    /**
     * 检查店铺级权限（员工是否有该店铺的访问权限）.
     */
    boolean canAccessShop(Long employeeId, Long shopId);

    /**
     * 刷新员工权限缓存.
     */
    void refreshEmployeePermissions(Long employeeId);
}
