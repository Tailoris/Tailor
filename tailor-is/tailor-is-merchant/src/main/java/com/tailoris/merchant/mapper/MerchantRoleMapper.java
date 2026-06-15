package com.tailoris.merchant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tailoris.merchant.entity.MerchantRole;
import org.apache.ibatis.annotations.Mapper;

/**
 * Merchant role data access layer
 * <p>商家角色数据访问层</p>
 *
 * @author Tailor IS Team
 * @since 2026-05-31
 */
@Mapper
public interface MerchantRoleMapper extends BaseMapper<MerchantRole> {
}
