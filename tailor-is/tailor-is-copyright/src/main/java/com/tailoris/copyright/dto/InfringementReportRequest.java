package com.tailoris.copyright.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "侵权举报请求")
public class InfringementReportRequest {

    @NotNull(message = "被举报商品ID不能为空")
    @Schema(description = "被举报商品ID")
    private Long reportedProductId;

    @NotNull(message = "被举报用户ID不能为空")
    @Schema(description = "被举报用户/商家ID")
    private Long reportedUserId;

    @NotNull(message = "侵权类型不能为空")
    @Schema(description = "侵权类型：1-盗用图片，2-抄袭设计，3-冒用品牌，4-仿冒商品，5-其他")
    private Integer infringementType;

    @NotBlank(message = "举报原因不能为空")
    @Schema(description = "举报原因简述")
    private String reason;

    @Schema(description = "侵权详细说明")
    private String description;

    @Schema(description = "举证图片(JSON数组)")
    private String evidenceImages;

    @Schema(description = "举证链接(JSON数组)")
    private String evidenceUrls;

    @Schema(description = "原创作品与侵权作品对比说明")
    private String comparisonDescription;

    @Schema(description = "关联版权登记ID")
    private Long copyrightId;

    @Schema(description = "紧急程度：1-普通，2-紧急，3-非常紧急")
    private Integer urgency;

    @Schema(description = "被举报店铺ID")
    private Long reportedShopId;

    @Schema(description = "被举报商家ID")
    private Long reportedMerchantId;
}
