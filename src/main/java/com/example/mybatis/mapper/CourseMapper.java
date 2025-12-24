package com.example.mybatis.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.mybatis.entity.Course;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * Course Mapper 接口 — 自定义 SQL 在 mapper XML 中实现
 * @author yihui
 */
@Mapper
public interface  CourseMapper extends BaseMapper<Course> {

    List<Course> selectByKeyword(@Param("keyword") String keyword);

    /**
     * 删除课程的所有亮点关联
     * @return 删除的行数
     */
    int deleteHighlightsByCourseId(@Param("courseId") Long courseId);

    /**
     * 插入课程-亮点关联
     * @return 插入的行数（1=成功，0=失败）
     */
    int insertCourseHighlight(@Param("courseId") Long courseId,
                              @Param("highlightId") Long highlightId);

    List<Map<String, Object>> selectHighlightsByCourseIds(@Param("courseIds") List<Long> courseIds);
}