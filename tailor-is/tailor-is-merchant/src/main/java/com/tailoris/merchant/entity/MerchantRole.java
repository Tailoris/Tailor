package com.tailoris.merchant.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.tailoris.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Merchant role entity
 * <p>商家角色实体，用于定义商家内部的权限角色（店主、店长、运营、客服、仓储等）</p>
 *
 * @author Tailor IS Team
 * @since 2026-05-31
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("merchant_role")
public class MerchantRole extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long merchantId;

    private String roleName;

    private String roleCode;

    private String description;

    /**
     * Permission codes (JSON array stored as string)
     */
    private String permissions;

    /**
     * Status: 0=disabled, 1=enabled
     */
    private Integer status;

    /**
     * Sort order
     */
    private Integer sortOrder;

    /**
     * Is system role (system roles cannot be deleted)
     */
    private Boolean systemRole;

    @TableLogic
    private Integer deleted;
}
