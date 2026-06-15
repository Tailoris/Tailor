package com.tailoris.order.dto;

import com.tailoris.common.dto.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class OrderQueryRequest extends PageRequest {

    private Integer status;

    private Integer productType;

    private String keyword;

    private Long merchantId;

    private Long shopId;
}
