package com.tailoris.api.admin.dto;

import com.tailoris.common.dto.PageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "店铺分类查询请求")
public class ShopCategoryQueryRequest extends PageRequest {

    @Schema(description = "关键词（分类名称/编码）")
    private String keyword;

    @Schema(description = "父分类ID")
    private Long parentId;

    @Schema(description = "状态：0-禁用，1-启用")
    private Integer status;
}
