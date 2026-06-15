package com.tailoris.user.dto;

import com.tailoris.common.validator.PhoneNumber;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "地址请求")
public class AddressRequest {

    @Schema(description = "收货人姓名")
    @NotBlank(message = "收货人姓名不能为空")
    private String name;

    @Schema(description = "收货人电话")
    @PhoneNumber(message = "手机号格式不正确")
    private String phone;

    @Schema(description = "省份")
    @NotBlank(message = "省份不能为空")
    private String province;

    @Schema(description = "城市")
    @NotBlank(message = "城市不能为空")
    private String city;

    @Schema(description = "区/县")
    @NotBlank(message = "区县不能为空")
    private String district;

    @Schema(description = "街道")
    private String street;

    @Schema(description = "详细地址")
    @NotBlank(message = "详细地址不能为空")
    private String detail;

    @Schema(description = "邮政编码")
    private String postalCode;

    @Schema(description = "是否默认地址：0-否，1-是")
    private Integer isDefault;

    @Schema(description = "地址标签")
    private String tag;
}
