package com.example.mybatis.service.impl;

import com.example.mybatis.entity.Course;
import com.example.mybatis.mapper.CourseMapper;
import com.example.mybatis.service.CourseService;
import com.example.mybatis.common.SpringException;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Course Service 实现类
 * 说明：
 * - 该类负责业务层处理，调用 Mapper 执行 XML 中定义的 SQL。
 * - 对重要操作进行异常捕获并包装为 SpringException（统一处理）。
 */
@Service
public class CourseServiceImpl extends ServiceImpl<CourseMapper, Course> implements CourseService {

    @Autowired
    private CourseMapper courseMapper;

    /**
     * 分页查询课程（基于 PageHelper）
     *
     * @param page     页码
     * @param pageSize 每页大小
     * @param keyword  查询关键字
     * @return PageInfo 包装的 Course 列表
     */
    public PageInfo<Course> pageCourses(int page, int pageSize, String keyword) {
        try {
            PageHelper.startPage(page, pageSize);
            List<Course> list = courseMapper.selectByKeyword(keyword);
            PageInfo<Course> pageInfo = new PageInfo<>(list);
            return pageInfo;
        } catch (Exception e) {
            // 捕获所有异常并包装为业务异常，便于 Controller 统一处理
            throw new SpringException("查询课程分页失败: " + e.getMessage(), 500, e);
        }
    }

    /**
     * 保存课程（演示：将 DTO 转换为 Entity 并保存）
     */
    public boolean saveCourse(Course course) {
        try {
            return this.save(course);
        } catch (Exception e) {
            throw new SpringException("保存课程失败: " + e.getMessage(), 500, e);
        }
    }

    /**
     * 更新课程（演示：先同步 highlights -> highlightStr）
     */
    public boolean updateCourse(Course course) {
        try {
            return this.updateById(course);
        } catch (Exception e) {
            throw new SpringException("更新课程失败: " + e.getMessage(), 500, e);
        }
    }
}
