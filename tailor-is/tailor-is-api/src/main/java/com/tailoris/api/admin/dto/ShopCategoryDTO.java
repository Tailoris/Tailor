package com.tailoris.api.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "店铺分类DTO")
public class ShopCategoryDTO {

    @Schema(description = "分类名称")
    @NotBlank(message = "分类名称不能为空")
    private String categoryName;

    @Schema(description = "父分类ID，顶级分类为0")
    @NotNull(message = "父分类ID不能为空")
    private Long parentId;

    @Schema(description = "分类编码")
    private String categoryCode;

    @Schema(description = "排序")
    private Integer sortOrder;

    @Schema(description = "佣金比例")
    private BigDecimal commissionRate;

    @Schema(description = "最低保证金")
    private BigDecimal depositMin;

    @Schema(description = "图标")
    private String icon;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "状态：0-禁用，1-启用")
    private Integer status;
}
