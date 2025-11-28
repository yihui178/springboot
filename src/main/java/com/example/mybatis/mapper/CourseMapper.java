package com.example.mybatis.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.mybatis.entity.Course;
import com.example.mybatis.entity.Highlight;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * Course Mapper 接口 — 自定义 SQL 在 mapper XML 中实现
 * @author yihui
 */
@Mapper
public interface CourseMapper extends BaseMapper<Course> {

    List<Course> selectByKeyword(@Param("keyword") String keyword);

    // 亮点关联操作
    void deleteHighlightsByCourseId(@Param("courseId") Long courseId);

    void insertCourseHighlight(@Param("courseId") Long courseId,
                               @Param("highlightId") Long highlightId);

    List<Map<String, Object>> selectHighlightsByCourseIds(@Param("courseIds") List<Long> courseIds);
}