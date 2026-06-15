package com.tailoris.merchant.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tailoris.merchant.entity.MerchantRole;

import java.util.List;

/**
 * Merchant role service interface
 * <p>商家角色服务接口</p>
 *
 * @author Tailor IS Team
 * @since 2026-05-31
 */
public interface IMerchantRoleService extends IService<MerchantRole> {

    /**
     * Create role for merchant
     *
     * @param role role data
     * @return created role
     */
    MerchantRole createRole(MerchantRole role);

    /**
     * Update role
     *
     * @param role role data
     * @return success
     */
    boolean updateRole(MerchantRole role);

    /**
     * Delete role (system roles cannot be deleted)
     *
     * @param roleId role ID
     * @param merchantId merchant ID (for verification)
     * @return success
     */
    boolean deleteRole(Long roleId, Long merchantId);

    /**
     * List roles by merchant
     *
     * @param merchantId merchant ID
     * @return role list
     */
    List<MerchantRole> listByMerchant(Long merchantId);

    /**
     * Get role with permissions
     *
     * @param roleId role ID
     * @return role
     */
    MerchantRole getRoleWithPermissions(Long roleId);
}
