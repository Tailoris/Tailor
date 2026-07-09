package com.tailoris.admin.feign;

import com.tailoris.admin.dto.CourseCreateRequest;
import com.tailoris.admin.dto.CourseQueryRequest;
import com.tailoris.admin.dto.CourseStatusUpdateRequest;
import com.tailoris.common.dto.PageResponse;
import com.tailoris.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "tailor-is-academy")
public interface AcademyFeignClient {

    @GetMapping("/api/v1/academy/courses")
    Result<PageResponse<Object>> getCourseList(@RequestBody CourseQueryRequest request);

    @GetMapping("/api/v1/academy/courses/{id}")
    Result<Object> getCourseById(@PathVariable Long id);

    @PostMapping("/api/v1/academy/courses")
    Result<Long> createCourse(@RequestBody CourseCreateRequest request);

    @PutMapping("/api/v1/academy/courses/{id}/status")
    Result<Void> updateCourseStatus(@PathVariable Long id, @RequestBody CourseStatusUpdateRequest request);

    @DeleteMapping("/api/v1/academy/courses/{id}")
    Result<Void> deleteCourse(@PathVariable Long id);

    @GetMapping("/api/v1/academy/instructors")
    Result<PageResponse<Object>> getInstructorList();

    @PutMapping("/api/v1/academy/instructors/{id}/audit")
    Result<Void> auditInstructor(@PathVariable Long id, @RequestParam Integer status, @RequestParam(required = false) String remark);
}