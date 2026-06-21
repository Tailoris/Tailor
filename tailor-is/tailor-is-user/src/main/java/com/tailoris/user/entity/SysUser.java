package com.tailoris.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.tailoris.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 系统用户实体类，对应 sys_user 表。
 *
 * <p>存储用户基本信息、实名认证信息、登录信息等</p>
 *
 * @author TailorIS Team
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
public class SysUser extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("username")
    private String username;

    @TableField("password")
    private String password;

    @TableField("phone")
    private String phone;

    @TableField("email")
    private String email;

    @TableField("avatar")
    private String avatar;

    @TableField("real_name")
    private String realName;

    @TableField("id_card")
    private String idCard;

    @TableField("status")
    private Integer status;

    @TableField("nick_name")
    private String nickName;

    @TableField("gender")
    private Integer gender;

    @TableField("birthday")
    private LocalDate birthday;

    @TableField("last_login_time")
    private LocalDateTime lastLoginTime;

    @TableField("last_login_ip")
    private String lastLoginIp;

    @TableField("id_card_front")
    private String idCardFront;

    @TableField("id_card_back")
    private String idCardBack;

    @TableField("certification")
    private Integer certification;

    @TableField("certification_time")
    private LocalDateTime certificationTime;
}
