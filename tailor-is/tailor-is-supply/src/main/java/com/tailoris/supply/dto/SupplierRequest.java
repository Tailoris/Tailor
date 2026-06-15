package com.tailoris.supply.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "供应商请求")
public class SupplierRequest {

    @NotBlank(message = "供应商名称不能为空")
    @Schema(description = "供应商名称")
    private String supplierName;

    @NotNull(message = "供应商类型不能为空")
    @Schema(description = "供应商类型：1-面料供应商，2-辅料供应商，3-加工厂，4-设计师")
    private Integer supplierType;

    @Schema(description = "Logo URL")
    private String logo;

    @Schema(description = "供应商描述")
    private String description;

    @Schema(description = "主要产品(JSON数组)")
    private String mainProducts;

    @Schema(description = "主营分类(JSON数组)")
    private String mainCategories;

    @Schema(description = "地址")
    private String location;

    @Schema(description = "省份")
    private String province;

    @Schema(description = "城市")
    private String city;

    @Schema(description = "最小起订量")
    private Integer minOrderQuantity;

    @Schema(description = "价格范围")
    private String priceRange;

    @Schema(description = "资质证书(JSON数组)")
    private String certifications;

    @Schema(description = "联系人姓名")
    private String contactName;

    @Schema(description = "联系电话")
    private String contactPhone;

    @Schema(description = "联系微信")
    private String contactWechat;
}
