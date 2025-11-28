package com.example.mybatis.service;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.mybatis.common.HttpResult;
import com.example.mybatis.dto.CourseDTO;
import com.example.mybatis.entity.Course;
import com.github.pagehelper.PageInfo;
import java.util.List;
import java.util.Map;
/**
 * 课程服务接口
 * @author yihui
 */
public interface CourseService extends IService<Course> {
    /**
     * 分页查询课程（返回 DTO，包含亮点信息）
     */
    PageInfo<CourseDTO> pageCoursesWithDTO(int page, int pageSize, String keyword);
    /**
     * 新增课程
     */
    HttpResult<String> addCourse(CourseDTO dto);
    /**
     * 更新课程
     */
    HttpResult<String> updateCourse(CourseDTO dto);
    /**
     * 删除课程
     */
    HttpResult<String> deleteCourse(Long courseId);
    /**
     * 批量查询多个课程的亮点（性能优化）
     */
    Map<Long, List<Long>> getHighlightsByCourseIds(List<Long> courseIds);
}