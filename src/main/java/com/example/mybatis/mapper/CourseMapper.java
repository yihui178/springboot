package com.example.mybatis.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.mybatis.entity.Course;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Course Mapper 接口 — 自定义 SQL 在 mapper XML 中实现
 */
@Mapper
public interface CourseMapper extends BaseMapper<Course> {
    /**
     * 根据关键字查询课程（用于分页）
     * 对应 resources/mapper/CourseMapper.xml 中的 SQL
     * @param keyword 搜索关键字
     * @return 课程列表（不分页，PageHelper 将自动拦截）
     */
    List<Course> selectByKeyword(@Param("keyword") String keyword);
}
