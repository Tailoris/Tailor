package com.tailoris.merchant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tailoris.merchant.entity.MerchantPermissionDict;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 商家权限按钮字典Mapper - MER-002.
 *
 * @author Tailor IS Team
 * @since 2026-06-03
 */
@Mapper
public interface MerchantPermissionDictMapper extends BaseMapper<MerchantPermissionDict> {

    @Select("SELECT * FROM merchant_permission_dict " +
            "WHERE is_enabled = 1 " +
            "ORDER BY module ASC, sort_order ASC")
    List<MerchantPermissionDict> selectAllEnabled();

    @Select("SELECT * FROM merchant_permission_dict " +
            "WHERE module = #{module} AND is_enabled = 1 " +
            "ORDER BY sort_order ASC")
    List<MerchantPermissionDict> selectByModule(@Param("module") String module);
}
