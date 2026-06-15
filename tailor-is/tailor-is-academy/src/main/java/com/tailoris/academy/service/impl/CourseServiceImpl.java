package com.tailoris.academy.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tailoris.academy.entity.Course;
import com.tailoris.academy.entity.CourseChapter;
import com.tailoris.academy.mapper.CourseChapterMapper;
import com.tailoris.academy.mapper.CourseMapper;
import com.tailoris.academy.service.CourseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CourseServiceImpl extends ServiceImpl<CourseMapper, Course> implements CourseService {

    private final CourseMapper courseMapper;
    private final CourseChapterMapper courseChapterMapper;

    @Override
    public Long createCourse(Course course) {
        courseMapper.insert(course);
        return course.getId();
    }

    @Override
    public void updateCourse(Long id, Course course) {
        course.setId(id);
        courseMapper.updateById(course);
    }

    @Override
    public void deleteCourse(Long id) {
        courseMapper.deleteById(id);
    }

    @Override
    public Course getCourseById(Long id) {
        return courseMapper.selectById(id);
    }

    @Override
    public List<Course> listCourses(int pageNum, int pageSize) {
        Page<Course> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Course> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(Course::getCreateTime);
        return courseMapper.selectPage(page, wrapper).getRecords();
    }

    @Override
    public List<CourseChapter> getChaptersByCourseId(Long courseId) {
        LambdaQueryWrapper<CourseChapter> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CourseChapter::getCourseId, courseId)
                .orderByAsc(CourseChapter::getSortOrder);
        return courseChapterMapper.selectList(wrapper);
    }

    @Override
    public void addChapter(CourseChapter chapter) {
        courseChapterMapper.insert(chapter);
    }

    @Override
    public void updateChapter(Long id, CourseChapter chapter) {
        chapter.setId(id);
        courseChapterMapper.updateById(chapter);
    }

    @Override
    public void deleteChapter(Long id) {
        courseChapterMapper.deleteById(id);
    }
}