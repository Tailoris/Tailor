package com.tailoris.pattern.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.tailoris.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pattern")
public class Pattern extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 版型名称 */
    @TableField("name")
    private String name;

    /** 版型描述 */
    @TableField("description")
    private String description;

    /** 版型分类 */
    @TableField("category")
    private String category;

    /** 版型图片地址 */
    @TableField("image_url")
    private String imageUrl;

    /** 规格尺寸（JSON格式） */
    @TableField("dimensions")
    private String dimensions;

    /** 状态：0禁用 1启用 */
    @TableField("status")
    private Integer status;

    /** 商户ID */
    @TableField("merchant_id")
    private Long merchantId;
}