package com.example.mybatis.service.impl;

import com.example.mybatis.entity.Course;
import com.example.mybatis.entity.Highlight;
import com.example.mybatis.mapper.CourseMapper;
import com.example.mybatis.service.CourseService;
import com.example.mybatis.common.SpringException;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    @Override
    public List<Highlight> getHighlightsByCourseId(Long courseId) {
        return courseMapper.selectHighlightsByCourseId(courseId);
    }
    /**
     * 🔥 批量查询多个课程的亮点（性能优化版本）
     * @param courseIds 课程 ID 列表
     * @return Map<课程ID, 亮点ID列表>
     */
    @Override
    public Map<Long, List<Long>> getHighlightsByCourseIds(List<Long> courseIds) {
        // 边界处理：如果课程列表为空，直接返回空 Map
        if (courseIds == null || courseIds.isEmpty()) {
            return Collections.emptyMap();
        }
        // 🔥 执行批量查询（只执行 1 次 SQL）
        List<Map<String, Object>> highlightMaps =
                courseMapper.selectHighlightsByCourseIds(courseIds);
        // 🔥 按 course_id 分组，返回 Map<课程ID, 亮点ID列表>
        return highlightMaps.stream()
                .collect(Collectors.groupingBy(
                        m -> ((Number) m.get("course_id")).longValue(), // 兼容 Integer/Long
                        Collectors.mapping(
                                m -> ((Number) m.get("id")).longValue(),
                                Collectors.toList()
                        )
                ));
    }

    @Override
    @Transactional
    public boolean saveCourse(Course course) {



        boolean ok = this.save(course);
        if (!ok) return false;

        saveOrUpdateHighlights(course);
        return true;
    }

    @Override
    @Transactional
    public boolean updateCourse(Course course) {
        boolean ok = this.updateById(course);
        if (!ok) return false;

        saveOrUpdateHighlights(course);
        return true;
    }


    private void saveOrUpdateHighlights(Course course) {
        Long courseId = course.getId();

        // 删除现有的亮点关联
        courseMapper.deleteHighlightsByCourseId(courseId);

        // 如果有亮点，就插入新的关联
        if (course.getHighlights() != null) {
            for (Highlight highlight : course.getHighlights()) {
                courseMapper.insertCourseHighlight(courseId, highlight.getId());
            }
        }
    }



}
