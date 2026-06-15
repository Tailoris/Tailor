package com.tailoris.merchant.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tailoris.merchant.entity.MerchantRoleTemplate;

import java.util.List;

/**
 * 商家角色权限模板服务接口 - MER-002.
 *
 * @author Tailor IS Team
 * @since 2026-06-03
 */
public interface IMerchantRoleTemplateService extends IService<MerchantRoleTemplate> {

    /**
     * 获取系统预设角色列表.
     */
    List<MerchantRoleTemplate> listSystemRoles();

    /**
     * 获取商家自定义角色列表.
     */
    List<MerchantRoleTemplate> listMerchantRoles(Long merchantId);

    /**
     * 获取角色（解析permissions为List<String>）.
     */
    MerchantRoleTemplate getRoleWithPermissions(Long id);

    /**
     * 按角色代码获取.
     */
    MerchantRoleTemplate getByRoleCode(String roleCode);

    /**
     * 创建商家自定义角色.
     */
    MerchantRoleTemplate createMerchantRole(Long merchantId, String roleName,
                                            List<String> permissions, String description);

    /**
     * 更新角色权限.
     */
    boolean updatePermissions(Long roleId, List<String> permissions);

    /**
     * 删除角色.
     */
    boolean deleteRole(Long roleId, Long merchantId);
}
