package com.tailoris.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;

@Data
@Schema(description = "用户更新请求")
public class UserUpdateRequest {

    @Schema(description = "昵称")
    private String nickName;

    @Schema(description = "头像URL")
    private String avatar;

    @Schema(description = "性别：0-未知，1-男，2-女")
    private Integer gender;

    @Schema(description = "生日")
    private LocalDate birthday;
}
