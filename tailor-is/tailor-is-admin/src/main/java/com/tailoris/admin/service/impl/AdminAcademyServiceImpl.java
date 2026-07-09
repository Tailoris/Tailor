package com.tailoris.admin.service.impl;

import com.tailoris.admin.dto.CourseCreateRequest;
import com.tailoris.admin.dto.CourseQueryRequest;
import com.tailoris.admin.dto.CourseStatusUpdateRequest;
import com.tailoris.admin.feign.AcademyFeignClient;
import com.tailoris.admin.service.AdminAcademyService;
import com.tailoris.common.dto.PageResponse;
import com.tailoris.common.exception.BusinessException;
import com.tailoris.common.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminAcademyServiceImpl implements AdminAcademyService {

    private final AcademyFeignClient academyFeignClient;

    @Override
    public PageResponse<Object> listCourses(CourseQueryRequest request) {
        Result<PageResponse<Object>> response = academyFeignClient.getCourseList(request);
        if (!response.isSuccess()) {
            throw new BusinessException(response.getMessage());
        }
        return response.getData();
    }

    @Override
    public Object getCourse(Long id) {
        Result<Object> response = academyFeignClient.getCourseById(id);
        if (!response.isSuccess()) {
            throw new BusinessException(response.getMessage());
        }
        return response.getData();
    }

    @Override
    public Long createCourse(CourseCreateRequest request) {
        Result<Long> response = academyFeignClient.createCourse(request);
        if (!response.isSuccess()) {
            throw new BusinessException(response.getMessage());
        }
        return response.getData();
    }

    @Override
    public void updateCourseStatus(Long id, CourseStatusUpdateRequest request) {
        Result<Void> response = academyFeignClient.updateCourseStatus(id, request);
        if (!response.isSuccess()) {
            throw new BusinessException(response.getMessage());
        }
    }

    @Override
    public void deleteCourse(Long id) {
        Result<Void> response = academyFeignClient.deleteCourse(id);
        if (!response.isSuccess()) {
            throw new BusinessException(response.getMessage());
        }
    }

    @Override
    public PageResponse<Object> listInstructors(Integer pageNum, Integer pageSize) {
        Result<PageResponse<Object>> response = academyFeignClient.getInstructorList();
        if (!response.isSuccess()) {
            throw new BusinessException(response.getMessage());
        }
        return response.getData();
    }

    @Override
    public void auditInstructor(Long id, Integer status, String remark) {
        Result<Void> response = academyFeignClient.auditInstructor(id, status, remark);
        if (!response.isSuccess()) {
            throw new BusinessException(response.getMessage());
        }
    }
}