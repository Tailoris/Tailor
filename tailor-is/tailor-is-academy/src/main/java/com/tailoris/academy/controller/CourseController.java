package com.tailoris.academy.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.tailoris.academy.entity.Course;
import com.tailoris.academy.entity.CourseChapter;
import com.tailoris.academy.service.CourseService;
import com.tailoris.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@SaCheckLogin
@Slf4j
@Tag(name = "课程管理")
@RestController
@RequestMapping("/api/v1/academy")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;

    @Operation(summary = "课程列表")
    @GetMapping("/courses")
    public Result<List<Course>> list(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        List<Course> courses = courseService.listCourses(pageNum, pageSize);
        return Result.success(courses);
    }

    @Operation(summary = "课程详情")
    @GetMapping("/courses/{id}")
    public Result<Course> getById(@PathVariable Long id) {
        Course course = courseService.getCourseById(id);
        return Result.success(course);
    }

    @Operation(summary = "课程章节列表")
    @GetMapping("/courses/{id}/chapters")
    public Result<List<CourseChapter>> listChapters(@PathVariable Long id) {
        List<CourseChapter> chapters = courseService.getChaptersByCourseId(id);
        return Result.success(chapters);
    }
}