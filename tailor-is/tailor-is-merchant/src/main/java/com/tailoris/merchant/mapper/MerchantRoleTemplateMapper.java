package com.tailoris.merchant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tailoris.merchant.entity.MerchantRoleTemplate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 商家角色权限模板Mapper - MER-002.
 *
 * @author Tailor IS Team
 * @since 2026-06-03
 */
@Mapper
public interface MerchantRoleTemplateMapper extends BaseMapper<MerchantRoleTemplate> {

    /**
     * 查询系统预设角色列表.
     */
    @Select("SELECT * FROM merchant_role_template " +
            "WHERE role_type = 1 AND is_enabled = 1 AND deleted = 0 " +
            "ORDER BY sort_order ASC")
    List<MerchantRoleTemplate> selectSystemRoles();

    /**
     * 查询某商家的自定义角色列表.
     */
    @Select("SELECT * FROM merchant_role_template " +
            "WHERE merchant_id = #{merchantId} AND role_type = 2 " +
            "  AND deleted = 0 " +
            "ORDER BY sort_order ASC")
    List<MerchantRoleTemplate> selectByMerchantId(@Param("merchantId") Long merchantId);

    /**
     * 查询系统预设角色 by roleCode.
     */
    @Select("SELECT * FROM merchant_role_template " +
            "WHERE role_code = #{roleCode} AND deleted = 0 LIMIT 1")
    MerchantRoleTemplate selectByRoleCode(@Param("roleCode") String roleCode);
}
