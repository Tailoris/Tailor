package com.tailoris.community.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "帖子创建请求 - 增强版（COM-001）")
public class PostCreateRequest {

    @NotBlank(message = "帖子标题不能为空")
    @Size(max = 200, message = "标题长度不能超过200")
    @Schema(description = "帖子标题")
    private String title;

    @NotBlank(message = "帖子内容不能为空")
    @Schema(description = "帖子内容（富文本HTML）")
    private String content;

    @Schema(description = "摘要")
    @Size(max = 500)
    private String summary;

    @Schema(description = "图片URL列表(JSON数组)")
    private String images;

    @Schema(description = "视频URL")
    private String videoUrl;

    @Schema(description = "帖子类型：1-图文 2-视频 3-纯文本 4-长文")
    private Integer type;

    @Schema(description = "分类ID")
    private Long categoryId;

    @Schema(description = "标签(JSON数组)")
    private String tags;

    @Schema(description = "话题ID列表")
    private List<Long> topicIds;

    @Schema(description = "关联商品ID")
    private Long relatedProductId;

    @Schema(description = "关联店铺ID")
    private Long relatedShopId;

    @Schema(description = "位置")
    private String location;

    @Schema(description = "经度")
    private java.math.BigDecimal longitude;

    @Schema(description = "纬度")
    private java.math.BigDecimal latitude;

    @Schema(description = "商品ID列表（用于商品种草）")
    private List<Long> productIds;
}
