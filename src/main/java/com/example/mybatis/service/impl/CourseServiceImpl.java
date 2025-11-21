package com.example.mybatis.service.impl;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.mybatis.common.SpringException;
import com.example.mybatis.dto.CourseDTO;
import com.example.mybatis.entity.Course;
import com.example.mybatis.entity.Highlight;
import com.example.mybatis.mapper.CourseMapper;
import com.example.mybatis.service.CourseService;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
/**
 * 课程服务实现类
 */
@Service
public class CourseServiceImpl extends ServiceImpl<CourseMapper, Course> implements CourseService {
    @Autowired
    private CourseMapper courseMapper;
    // ========== 公共接口实现 ==========
    /**
     * 分页查询课程（返回 DTO，包含亮点信息）
     */
    @Override
    public PageInfo<CourseDTO> pageCoursesWithDTO(int page, int pageSize, String keyword) {
        try {
            // 1. 分页查询课程实体
            PageHelper.startPage(page, pageSize);
            List<Course> courseList = courseMapper.selectByKeyword(keyword);
            PageInfo<Course> coursePageInfo = new PageInfo<>(courseList);
            // 2. 边界处理：如果课程列表为空，直接返回空分页结果
            if (courseList.isEmpty()) {
                PageInfo<CourseDTO> emptyPage = new PageInfo<>();
                emptyPage.setList(Collections.emptyList());
                emptyPage.setPageNum(coursePageInfo.getPageNum());
                emptyPage.setPageSize(coursePageInfo.getPageSize());
                emptyPage.setTotal(coursePageInfo.getTotal());
                emptyPage.setPages(coursePageInfo.getPages());
                return emptyPage;
            }
            // 3. 批量查询所有课程的亮点（性能优化：只执行 1 次 SQL）
            List<Long> courseIds = courseList.stream()
                    .map(Course::getId)
                    .collect(Collectors.toList());
            Map<Long, List<Long>> courseHighlightMap = getHighlightsByCourseIds(courseIds);
            // 4. 转换为 DTO
            List<CourseDTO> dtoList = courseList.stream().map(course -> {
                CourseDTO dto = new CourseDTO();
                BeanUtils.copyProperties(course, dto);
                dto.setHighlightIds(
                        courseHighlightMap.getOrDefault(course.getId(), Collections.emptyList())
                );
                return dto;
            }).collect(Collectors.toList());
            // 5. 封装分页结果
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
    /**
     * 新增课程
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addCourse(CourseDTO dto) {
        // 1. DTO 转 Entity
        Course course = convertToEntity(dto);
        // 2. 保存课程
        if (!saveCourse(course)) {
            throw new SpringException("新增课程失败", 500);
        }
    }
    /**
     * 更新课程
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCourse(CourseDTO dto) {
        // 1. 校验课程 ID
        if (dto.getId() == null) {
            throw new SpringException("课程ID不能为空", 400);
        }
        // 2. 检查课程是否存在
        Course existingCourse = this.getById(dto.getId());
        if (existingCourse == null) {
            throw new SpringException("课程不存在", 404);
        }
        // 3. DTO 转 Entity
        Course course = convertToEntity(dto);
        // 4. 更新课程
        if (!this.updateById(course)) {
            throw new SpringException("更新课程失败", 500);
        }
        // 5. 更新亮点关联
        saveOrUpdateHighlights(course);
    }
    /**
     * 删除课程
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCourse(Long courseId) {
        // 1. 校验课程 ID
        if (courseId == null) {
            throw new SpringException("课程ID不能为空", 400);
        }
        // 2. 检查课程是否存在
        Course existingCourse = this.getById(courseId);
        if (existingCourse == null) {
            throw new SpringException("课程不存在", 404);
        }
        // 3. 删除课程（级联删除亮点关联）
        if (!this.removeById(courseId)) {
            throw new SpringException("删除课程失败", 500);
        }
    }
    /**
     * 保存课程（原有方法，保留用于内部调用）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveCourse(Course course) {
        boolean ok = this.save(course);
        if (!ok) return false;
        saveOrUpdateHighlights(course);
        return true;
    }
    /**
     * 查询单个课程的亮点
     */
    @Override
    public List<Highlight> getHighlightsByCourseId(Long courseId) {
        return courseMapper.selectHighlightsByCourseId(courseId);
    }
    /**
     * 批量查询多个课程的亮点（性能优化版本）
     */
    @Override
    public Map<Long, List<Long>> getHighlightsByCourseIds(List<Long> courseIds) {
        // 边界处理：如果课程列表为空，直接返回空 Map
        if (courseIds == null || courseIds.isEmpty()) {
            return Collections.emptyMap();
        }
        // 执行批量查询（只执行 1 次 SQL）
        List<Map<String, Object>> highlightMaps = courseMapper.selectHighlightsByCourseIds(courseIds);
        // 按 course_id 分组，返回 Map<课程ID, 亮点ID列表>
        return highlightMaps.stream()
                .collect(Collectors.groupingBy(
                        m -> ((Number) m.get("course_id")).longValue(),
                        Collectors.mapping(
                                m -> ((Number) m.get("id")).longValue(),
                                Collectors.toList()
                        )
                ));
    }
    // ========== 私有方法：业务逻辑封装 ==========
    /**
     * DTO 转 Entity
     */
    private Course convertToEntity(CourseDTO dto) {
        Course course = new Course();
        BeanUtils.copyProperties(dto, course);
        // 转换亮点 ID 列表
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
    /**
     * 保存或更新课程的亮点关联
     */
    private void saveOrUpdateHighlights(Course course) {
        Long courseId = course.getId();
        // 1. 删除现有的亮点关联
        courseMapper.deleteHighlightsByCourseId(courseId);
        // 2. 如果有亮点，插入新的关联
        if (course.getHighlights() != null && !course.getHighlights().isEmpty()) {
            for (Highlight highlight : course.getHighlights()) {
                courseMapper.insertCourseHighlight(courseId, highlight.getId());
            }
        }
    }
}