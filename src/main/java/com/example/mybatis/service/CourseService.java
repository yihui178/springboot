package com.example.mybatis.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.mybatis.entity.Course;
import com.example.mybatis.entity.Highlight;

import java.util.List;
import java.util.Map;

public interface CourseService extends IService<Course> {
    boolean saveCourse(Course course);
    boolean updateCourse(Course course);

    List<Highlight> getHighlightsByCourseId(Long courseId);
    Map<Long, List<Long>> getHighlightsByCourseIds(List<Long> courseIds);
}
