package com.tailoris.admin.service;

import com.tailoris.admin.dto.CourseCreateRequest;
import com.tailoris.admin.dto.CourseQueryRequest;
import com.tailoris.admin.dto.CourseStatusUpdateRequest;
import com.tailoris.common.dto.PageResponse;

public interface AdminAcademyService {

    PageResponse<Object> listCourses(CourseQueryRequest request);

    Object getCourse(Long id);

    Long createCourse(CourseCreateRequest request);

    void updateCourseStatus(Long id, CourseStatusUpdateRequest request);

    void deleteCourse(Long id);

    PageResponse<Object> listInstructors(Integer pageNum, Integer pageSize);

    void auditInstructor(Long id, Integer status, String remark);
}