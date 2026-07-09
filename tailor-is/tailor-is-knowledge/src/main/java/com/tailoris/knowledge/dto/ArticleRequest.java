package com.tailoris.knowledge.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ArticleRequest {

    @NotBlank(message = "文章标题不能为空")
    @Size(max = 200, message = "文章标题不能超过200个字符")
    private String title;

    @Size(max = 500, message = "文章摘要不能超过500个字符")
    private String summary;

    private String content;

    private String coverImage;

    private Long categoryId;

    private String tagIds;

    private Integer isTop;

    private Integer isRecommend;

    private String sourceType;

    private String sourceUrl;
}