package com.tailoris.academy.dto;

import lombok.Data;

@Data
public class ChapterUpdateRequest {

    private String title;

    private String videoUrl;

    private Integer duration;

    private Integer sortOrder;
}