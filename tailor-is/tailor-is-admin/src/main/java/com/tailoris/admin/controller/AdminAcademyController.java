package com.tailoris.admin.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.tailoris.admin.dto.CourseCreateRequest;
import com.tailoris.common.constant.RoutePortConstant;
import com.tailoris.admin.dto.CourseQueryRequest;
import com.tailoris.admin.dto.CourseStatusUpdateRequest;
import com.tailoris.admin.service.AdminAcademyService;
import com.tailoris.common.dto.PageResponse;
import com.tailoris.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@SaCheckRole({"SUPER_ADMIN", "PLATFORM_ADMIN", "ACADEMY_ADMIN"})
@Tag(name = "线上学堂管理")
@RestController
@RequestMapping(RoutePortConstant.ROUTE_ADMIN_ACADEMY)
@RequiredArgsConstructor
public class AdminAcademyController {

    private final AdminAcademyService adminAcademyService;

    @Operation(summary = "查询课程列表")
    @GetMapping("/courses")
    public Result<PageResponse<Object>> listCourses(CourseQueryRequest request) {
        PageResponse<Object> page = adminAcademyService.listCourses(request);
        return Result.success(page);
    }

    @Operation(summary = "获取课程详情")
    @GetMapping("/courses/{id}")
    public Result<Object> getCourse(@Parameter(description = "课程ID") @PathVariable Long id) {
        Object course = adminAcademyService.getCourse(id);
        return Result.success(course);
    }

    @Operation(summary = "创建课程")
    @PostMapping("/courses")
    public Result<Long> createCourse(@RequestBody CourseCreateRequest request) {
        Long id = adminAcademyService.createCourse(request);
        return Result.success(id);
    }

    @Operation(summary = "更新课程状态")
    @PutMapping("/courses/{id}/status")
    public Result<Void> updateCourseStatus(
            @Parameter(description = "课程ID") @PathVariable Long id,
            @RequestBody CourseStatusUpdateRequest request) {
        adminAcademyService.updateCourseStatus(id, request);
        return Result.success();
    }

    @Operation(summary = "删除课程")
    @DeleteMapping("/courses/{id}")
    public Result<Void> deleteCourse(@Parameter(description = "课程ID") @PathVariable Long id) {
        adminAcademyService.deleteCourse(id);
        return Result.success();
    }

    @Operation(summary = "查询讲师列表")
    @GetMapping("/instructors")
    public Result<PageResponse<Object>> listInstructors(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") Integer pageSize) {
        PageResponse<Object> page = adminAcademyService.listInstructors(pageNum, pageSize);
        return Result.success(page);
    }

    @Operation(summary = "审核讲师")
    @PutMapping("/instructors/{id}/audit")
    public Result<Void> auditInstructor(
            @Parameter(description = "讲师ID") @PathVariable Long id,
            @Parameter(description = "审核状态") @RequestParam Integer status,
            @Parameter(description = "审核备注") @RequestParam(required = false) String remark) {
        adminAcademyService.auditInstructor(id, status, remark);
        return Result.success();
    }
}