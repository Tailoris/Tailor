package com.tailoris.academy.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CourseCreateRequest {

    @NotBlank(message = "课程标题不能为空")
    private String title;

    private String description;

    private String coverImage;

    private String videoUrl;

    private Long categoryId;

    private Integer duration;

    private Integer status = 1;

    private Long authorId;
}