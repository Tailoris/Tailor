package com.tailoris.knowledge.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CategoryRequest {

    @NotBlank(message = "分类名称不能为空")
    @Size(max = 100, message = "分类名称不能超过100个字符")
    private String name;

    private Long parentId;

    @Size(max = 500, message = "分类描述不能超过500个字符")
    private String description;

    private String icon;

    private Integer sortOrder;
}