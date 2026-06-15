package com.tailoris.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CategoryRequest {

    @NotBlank(message = "分类名称不能为空")
    private String name;

    private Long parentId;

    @NotNull(message = "分类层级不能为空")
    private Integer level;

    private Integer sort;

    private String icon;

    private String image;

    private Integer status;

    private String description;
}
