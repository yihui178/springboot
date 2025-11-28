package com.example.mybatis.service.impl;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.mybatis.common.HttpResult;
import com.example.mybatis.common.SpringException;
import com.example.mybatis.dto.CourseDTO;
import com.example.mybatis.entity.Course;
import com.example.mybatis.entity.Highlight;
import com.example.mybatis.mapper.CourseMapper;
import com.example.mybatis.service.CourseService;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
/**
 * 课程服务实现类
 * @author yihui
 */
@Service
@RequiredArgsConstructor
public class CourseServiceImpl extends ServiceImpl<CourseMapper, Course> implements CourseService {

    private final CourseMapper courseMapper;

    // ========== 查询操作 ==========
    @Override
    public PageInfo<CourseDTO> pageCoursesWithDTO(int page, int pageSize, String keyword) {
        try {
            PageHelper.startPage(page, pageSize);
            List<Course> courseList = courseMapper.selectByKeyword(keyword);
            PageInfo<Course> coursePageInfo = new PageInfo<>(courseList);
            if (courseList.isEmpty()) {
                PageInfo<CourseDTO> emptyPage = new PageInfo<>();
                emptyPage.setList(Collections.emptyList());
                emptyPage.setPageNum(coursePageInfo.getPageNum());
                emptyPage.setPageSize(coursePageInfo.getPageSize());
                emptyPage.setTotal(coursePageInfo.getTotal());
                emptyPage.setPages(coursePageInfo.getPages());
                return emptyPage;
            }
            List<Long> courseIds = courseList.stream()
                    .map(Course::getId)
                    .collect(Collectors.toList());
            Map<Long, List<Long>> courseHighlightMap = getHighlightsByCourseIds(courseIds);
            List<CourseDTO> dtoList = courseList.stream().map(course -> {
                CourseDTO dto = new CourseDTO();
                BeanUtils.copyProperties(course, dto);
                dto.setHighlightIds(
                        courseHighlightMap.getOrDefault(course.getId(), Collections.emptyList())
                );
                return dto;
            }).collect(Collectors.toList());
            PageInfo<CourseDTO> dtoPage = new PageInfo<>();
            dtoPage.setList(dtoList);
            dtoPage.setPageNum(coursePageInfo.getPageNum());
            dtoPage.setPageSize(coursePageInfo.getPageSize());
            dtoPage.setTotal(coursePageInfo.getTotal());
            dtoPage.setPages(coursePageInfo.getPages());
            return dtoPage;
        } catch (Exception e) {
            throw new SpringException("查询课程分页失败: " + e.getMessage(), 500, e);
        }
    }
    // ========== CUD 操作 ==========
    /**
     * 新增课程
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public HttpResult<String> addCourse(CourseDTO dto) {
        Course course = convertToEntity(dto);

        boolean success = this.save(course);
        if (!success) {
            return HttpResult.error(500, "新增课程失败");
        }

        saveOrUpdateHighlights(course);

        return HttpResult.ok("新增成功");
    }
    /**
     * 更新课程
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public HttpResult<String> updateCourse(CourseDTO dto) {
        if (dto.getId() == null) {
            return HttpResult.error(400, "课程ID不能为空");
        }

        Course existingCourse = this.getById(dto.getId());
        if (existingCourse == null) {
            return HttpResult.error(404, "课程不存在");
        }

        Course course = convertToEntity(dto);
        boolean success = this.updateById(course);

        if (!success) {
            return HttpResult.error(500, "更新课程失败");
        }

        saveOrUpdateHighlights(course);

        return HttpResult.ok("更新成功");
    }
    /**
     * 删除课程
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public HttpResult<String> deleteCourse(Long courseId) {
        if (courseId == null) {
            return HttpResult.error(400, "课程ID不能为空");
        }

        Course existingCourse = this.getById(courseId);
        if (existingCourse == null) {
            return HttpResult.error(404, "课程不存在");
        }

        boolean success = this.removeById(courseId);
        if (!success) {
            return HttpResult.error(500, "删除课程失败");
        }

        return HttpResult.ok("删除成功");
    }
    // ========== 辅助方法 ==========
    @Override
    public Map<Long, List<Long>> getHighlightsByCourseIds(List<Long> courseIds) {
        if (courseIds == null || courseIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Map<String, Object>> highlightMaps = courseMapper.selectHighlightsByCourseIds(courseIds);
        return highlightMaps.stream()
                .collect(Collectors.groupingBy(
                        m -> ((Number) m.get("course_id")).longValue(),
                        Collectors.mapping(
                                m -> ((Number) m.get("id")).longValue(),
                                Collectors.toList()
                        )
                ));
    }
    private Course convertToEntity(CourseDTO dto) {
        Course course = new Course();
        BeanUtils.copyProperties(dto, course);
        if (dto.getHighlightIds() != null && !dto.getHighlightIds().isEmpty()) {
            List<Highlight> highlights = dto.getHighlightIds().stream()
                    .map(id -> {
                        Highlight h = new Highlight();
                        h.setId(id);
                        return h;
                    })
                    .collect(Collectors.toList());
            course.setHighlights(highlights);
        }
        return course;
    }
    private void saveOrUpdateHighlights(Course course) {
        Long courseId = course.getId();
        courseMapper.deleteHighlightsByCourseId(courseId);
        if (course.getHighlights() != null && !course.getHighlights().isEmpty()) {
            for (Highlight highlight : course.getHighlights()) {
                courseMapper.insertCourseHighlight(courseId, highlight.getId());
            }
        }
    }
}