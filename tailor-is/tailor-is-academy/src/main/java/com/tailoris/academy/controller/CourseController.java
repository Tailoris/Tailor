package com.tailoris.academy.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.tailoris.academy.dto.ChapterCreateRequest;
import com.tailoris.academy.dto.ChapterUpdateRequest;
import com.tailoris.academy.dto.CourseCreateRequest;
import com.tailoris.academy.dto.CourseUpdateRequest;
import com.tailoris.academy.entity.Course;
import com.tailoris.academy.entity.CourseChapter;
import com.tailoris.academy.service.CourseService;
import com.tailoris.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.tailoris.common.constant.RoutePortConstant;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@SaCheckLogin
@Slf4j
@Tag(name = "课程管理")
@RestController
@RequestMapping(RoutePortConstant.ROUTE_ACADEMY)
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

    @Operation(summary = "创建课程")
    @PostMapping("/courses")
    public Result<Long> create(@Valid @RequestBody CourseCreateRequest request) {
        Course course = new Course();
        course.setTitle(request.getTitle());
        course.setDescription(request.getDescription());
        course.setCoverImage(request.getCoverImage());
        course.setVideoUrl(request.getVideoUrl());
        course.setCategoryId(request.getCategoryId());
        course.setDuration(request.getDuration());
        course.setStatus(request.getStatus());
        course.setAuthorId(request.getAuthorId());
        Long id = courseService.createCourse(course);
        return Result.success(id);
    }

    @Operation(summary = "更新课程")
    @PutMapping("/courses/{id}")
    public Result<Void> update(
            @PathVariable Long id,
            @RequestBody CourseUpdateRequest request) {
        Course course = new Course();
        if (request.getTitle() != null) course.setTitle(request.getTitle());
        if (request.getDescription() != null) course.setDescription(request.getDescription());
        if (request.getCoverImage() != null) course.setCoverImage(request.getCoverImage());
        if (request.getVideoUrl() != null) course.setVideoUrl(request.getVideoUrl());
        if (request.getCategoryId() != null) course.setCategoryId(request.getCategoryId());
        if (request.getDuration() != null) course.setDuration(request.getDuration());
        if (request.getStatus() != null) course.setStatus(request.getStatus());
        courseService.updateCourse(id, course);
        return Result.success();
    }

    @Operation(summary = "删除课程")
    @DeleteMapping("/courses/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        courseService.deleteCourse(id);
        return Result.success();
    }

    @Operation(summary = "课程章节列表")
    @GetMapping("/courses/{id}/chapters")
    public Result<List<CourseChapter>> listChapters(@PathVariable Long id) {
        List<CourseChapter> chapters = courseService.getChaptersByCourseId(id);
        return Result.success(chapters);
    }

    @Operation(summary = "添加章节")
    @PostMapping("/chapters")
    public Result<Long> addChapter(@Valid @RequestBody ChapterCreateRequest request) {
        CourseChapter chapter = new CourseChapter();
        chapter.setCourseId(request.getCourseId());
        chapter.setTitle(request.getTitle());
        chapter.setVideoUrl(request.getVideoUrl());
        chapter.setDuration(request.getDuration());
        chapter.setSortOrder(request.getSortOrder());
        courseService.addChapter(chapter);
        return Result.success(chapter.getId());
    }

    @Operation(summary = "更新章节")
    @PutMapping("/chapters/{id}")
    public Result<Void> updateChapter(
            @PathVariable Long id,
            @RequestBody ChapterUpdateRequest request) {
        CourseChapter chapter = new CourseChapter();
        if (request.getTitle() != null) chapter.setTitle(request.getTitle());
        if (request.getVideoUrl() != null) chapter.setVideoUrl(request.getVideoUrl());
        if (request.getDuration() != null) chapter.setDuration(request.getDuration());
        if (request.getSortOrder() != null) chapter.setSortOrder(request.getSortOrder());
        courseService.updateChapter(id, chapter);
        return Result.success();
    }

    @Operation(summary = "删除章节")
    @DeleteMapping("/chapters/{id}")
    public Result<Void> deleteChapter(@PathVariable Long id) {
        courseService.deleteChapter(id);
        return Result.success();
    }
}