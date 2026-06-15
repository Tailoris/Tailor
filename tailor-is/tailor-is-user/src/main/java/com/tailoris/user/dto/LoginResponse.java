package com.tailoris.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@Schema(description = "登录响应")
public class LoginResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "访问令牌")
    private String token;

    @Schema(description = "用户信息")
    private UserInfo userInfo;

    /** 🔒 USR-003: 是否新注册用户（首次登录） */
    @Schema(description = "是否新用户（首次登录）")
    private Boolean isNewUser;

    @Data
    @Schema(description = "用户信息")
    public static class UserInfo implements Serializable {

        private static final long serialVersionUID = 1L;

        @Schema(description = "用户ID")
        private Long id;

        @Schema(description = "用户名")
        private String username;

        @Schema(description = "手机号")
        private String phone;

        @Schema(description = "邮箱")
        private String email;

        @Schema(description = "头像URL")
        private String avatar;

        @Schema(description = "昵称")
        private String nickName;

        @Schema(description = "真实姓名")
        private String realName;

        @Schema(description = "性别：0-未知，1-男，2-女")
        private Integer gender;

        @Schema(description = "状态：0-禁用，1-启用")
        private Integer status;

        @Schema(description = "角色列表")
        private List<String> roles;

        @Schema(description = "权限编码列表")
        private List<String> permissions;
    }
}
