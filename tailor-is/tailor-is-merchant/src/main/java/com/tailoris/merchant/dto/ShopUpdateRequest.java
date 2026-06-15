package com.tailoris.merchant.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "店铺更新请求")
public class ShopUpdateRequest {

    @Schema(description = "店铺名称")
    @Size(max = 128, message = "店铺名称不能超过128个字符")
    private String shopName;

    @Schema(description = "店铺Logo URL")
    private String shopLogo;

    @Schema(description = "店铺Banner URL")
    private String shopBanner;

    @Schema(description = "店铺描述")
    @Size(max = 512, message = "店铺描述不能超过512个字符")
    private String shopDesc;

    @Schema(description = "店铺装修配置（JSON格式）")
    private String decorationConfig;

    @Schema(description = "店铺主题模板")
    private String shopTheme;

    @Schema(description = "店铺公告")
    @Size(max = 512, message = "店铺公告不能超过512个字符")
    private String announcement;

    @Schema(description = "客服联系方式")
    private String contactService;

    @Schema(description = "所在省份")
    private String province;

    @Schema(description = "所在城市")
    private String city;

    @Schema(description = "所在区县")
    private String district;

    @Schema(description = "详细地址")
    private String address;

    @Schema(description = "经度")
    private Double longitude;

    @Schema(description = "纬度")
    private Double latitude;
}
