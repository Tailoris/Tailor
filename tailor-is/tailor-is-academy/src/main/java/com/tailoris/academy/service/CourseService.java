package com.tailoris.academy.service;

import com.tailoris.academy.entity.Course;
import com.tailoris.academy.entity.CourseChapter;

import java.util.List;

public interface CourseService {

    Long createCourse(Course course);

    void updateCourse(Long id, Course course);

    void deleteCourse(Long id);

    Course getCourseById(Long id);

    List<Course> listCourses(int pageNum, int pageSize);

    List<CourseChapter> getChaptersByCourseId(Long courseId);

    void addChapter(CourseChapter chapter);

    void updateChapter(Long id, CourseChapter chapter);

    void deleteChapter(Long id);
}