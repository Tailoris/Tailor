package com.tailoris.admin.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CourseCreateRequest {

    @NotBlank(message = "课程名称不能为空")
    private String courseName;

    private String description;

    private String coverImage;

    private Long instructorId;

    private Integer categoryId;

    private Integer duration;

    private Integer difficulty;

    private Integer price;
}