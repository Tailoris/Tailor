package com.tailoris.knowledge.dto;

import com.tailoris.common.dto.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ArticleQueryRequest extends PageRequest {

    private String title;

    private Long categoryId;

    private String tagId;

    private Integer isTop;

    private Integer isRecommend;

    private Integer status;

    private String sortBy;

    private String sortOrder;
}