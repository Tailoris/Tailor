package com.tailoris.product.dto;

import com.tailoris.common.dto.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ProductQueryRequest extends PageRequest {

    private String keyword;

    private Long categoryId;

    private Long merchantId;

    private Long shopId;

    private Integer productType;

    private Integer status;

    private Integer auditStatus;

    private Integer copyrightFlag;

    private Long brandId;

    private Long tagId;

    private String orderByField;
}
