package com.tailoris.design.dto;

import com.tailoris.common.dto.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class DesignQueryRequest extends PageRequest {

    private String name;

    private String designType;

    private String styleCategory;

    private String color;

    private String material;

    private String difficulty;

    private Integer isFree;

    private Integer status;

    private Long userId;

    private Long merchantId;

    private String sortBy;

    private String sortOrder;
}