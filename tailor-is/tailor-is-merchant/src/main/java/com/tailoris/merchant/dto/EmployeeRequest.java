package com.tailoris.merchant.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "员工请求")
public class EmployeeRequest {

    @Schema(description = "店铺ID（为空表示管理所有店铺）")
    private Long shopId;

    @Schema(description = "关联用户ID")
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @Schema(description = "员工姓名")
    @NotBlank(message = "员工姓名不能为空")
    private String employeeName;

    @Schema(description = "员工电话")
    private String employeePhone;

    @Schema(description = "员工角色：1-店长，2-运营，3-客服，4-库管，5-财务")
    @NotNull(message = "员工角色不能为空")
    private Integer role;

    @Schema(description = "权限配置（JSON格式）")
    private String permissions;
}
