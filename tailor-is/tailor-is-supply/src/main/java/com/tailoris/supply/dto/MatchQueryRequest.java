package com.tailoris.supply.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "匹配查询请求")
public class MatchQueryRequest {

    @Schema(description = "帖子类型：1-供应，2-需求")
    private Integer postType;

    @Schema(description = "分类ID")
    private Long categoryId;

    @Schema(description = "省份")
    private String province;

    @Schema(description = "城市")
    private String city;

    @Schema(description = "材质类型")
    private String materialType;
}
