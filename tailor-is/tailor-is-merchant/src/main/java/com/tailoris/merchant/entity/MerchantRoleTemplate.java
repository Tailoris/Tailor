package com.tailoris.merchant.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.tailoris.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 商家员工角色权限模板 - MER-002.
 *
 * <p>定义商家的角色与按钮级权限映射关系，支持系统预设与商家自定义两种类型。</p>
 *
 * <h3>角色类型 role_type</h3>
 * <ul>
 *   <li>1: 系统预设（平台统一定义，商家不可修改）</li>
 *   <li>2: 商家自定义（商家自行创建与维护）</li>
 * </ul>
 *
 * <h3>权限编码格式</h3>
 * <p>采用 <code>模块:动作</code> 格式，例如：<code>product:create</code></p>
 *
 * @author Tailor IS Team
 * @since 2026-06-03
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("merchant_role_template")
public class MerchantRoleTemplate extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 角色代码 */
    @TableField("role_code")
    private String roleCode;

    /** 角色名称 */
    @TableField("role_name")
    private String roleName;

    /** 角色类型 1=系统预设 2=商家自定义 */
    @TableField("role_type")
    private Integer roleType;

    /** 商家ID（自定义角色时填写） */
    @TableField("merchant_id")
    private Long merchantId;

    /** 权限列表（JSON数组） */
    @TableField("permissions")
    private String permissions;

    /** 角色描述 */
    @TableField("description")
    private String description;

    /** 排序 */
    @TableField("sort_order")
    private Integer sortOrder;

    /** 是否启用 0否 1是 */
    @TableField("is_enabled")
    private Integer isEnabled;

    @TableLogic
    @TableField("deleted")
    private Integer deleted;

    /**
     * 解析后的权限列表（业务层使用）.
     */
    @TableField(exist = false)
    private List<String> permissionList;
}
