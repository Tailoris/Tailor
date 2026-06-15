package com.tailoris.merchant.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 商家权限按钮字典 - MER-002.
 *
 * <p>用于前端按权限码控制按钮的显示与隐藏。</p>
 *
 * @author Tailor IS Team
 * @since 2026-06-03
 */
@Data
@TableName("merchant_permission_dict")
public class MerchantPermissionDict implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 权限代码（唯一） */
    @TableField("permission_code")
    private String permissionCode;

    /** 权限名称 */
    @TableField("permission_name")
    private String permissionName;

    /** 所属模块 */
    @TableField("module")
    private String module;

    /** 权限描述 */
    @TableField("description")
    private String description;

    /** 排序 */
    @TableField("sort_order")
    private Integer sortOrder;

    /** 是否启用 */
    @TableField("is_enabled")
    private Integer isEnabled;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;
}
