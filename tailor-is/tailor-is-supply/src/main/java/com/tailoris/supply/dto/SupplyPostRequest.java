package com.tailoris.supply.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Schema(description = "供需帖子请求")
public class SupplyPostRequest {

    @NotNull(message = "帖子类型不能为空")
    @Schema(description = "帖子类型：1-供应，2-需求")
    private Integer postType;

    @NotBlank(message = "标题不能为空")
    @Schema(description = "帖子标题")
    private String title;

    @NotBlank(message = "内容不能为空")
    @Schema(description = "帖子内容")
    private String content;

    @Schema(description = "图片URL列表(JSON数组)")
    private String images;

    @Schema(description = "分类ID")
    private Long categoryId;

    @Schema(description = "材质类型")
    private String materialType;

    @Schema(description = "数量")
    private Integer quantity;

    @Schema(description = "单价")
    private BigDecimal unitPrice;

    @Schema(description = "地区")
    private String location;

    @Schema(description = "省份")
    private String province;

    @Schema(description = "城市")
    private String city;

    @Schema(description = "联系人姓名")
    private String contactName;

    @Schema(description = "联系电话")
    private String contactPhone;

    @Schema(description = "联系微信")
    private String contactWechat;

    @Schema(description = "截止日期")
    private LocalDate expireDate;

    @Schema(description = "是否置顶")
    private Integer isTop;

    @Schema(description = "是否紧急")
    private Integer isUrgent;
}
