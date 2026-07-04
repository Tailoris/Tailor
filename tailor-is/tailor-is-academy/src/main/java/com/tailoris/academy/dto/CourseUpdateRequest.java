package com.tailoris.academy.dto;

import lombok.Data;

@Data
public class CourseUpdateRequest {

    private String title;

    private String description;

    private String coverImage;

    private String videoUrl;

    private Long categoryId;

    private Integer duration;

    private Integer status;
}