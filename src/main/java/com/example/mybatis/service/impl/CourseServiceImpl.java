package com.example.mybatis.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.mybatis.common.HttpResult;
import com.example.mybatis.common.SpringException;
import com.example.mybatis.dto.CourseDTO;
import com.example.mybatis.entity.Activity; // ✅ 导入
import com.example.mybatis.entity.Course;
import com.example.mybatis.entity.Highlight;
import com.example.mybatis.mapper.CourseMapper;
import com.example.mybatis.service.ActivityService; // ✅ 导入
import com.example.mybatis.service.CourseService;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 课程服务实现类
 * @author yihui
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CourseServiceImpl extends ServiceImpl<CourseMapper, Course> implements CourseService {

    private final CourseMapper courseMapper;
    private final ActivityService activityService; // ✅ 注入 ActivityService

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

        // ✅ 自动创建对应的培训活动
        createTrainingActivity(course);

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

        // ✅ 同步更新关联的培训活动
        updateTrainingActivity(course);

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

        // ✅ 检查是否有关联的培训活动报名
        Activity relatedActivity = activityService.lambdaQuery()
                .eq(Activity::getCourseId, courseId)
                .one();

        if (relatedActivity != null && relatedActivity.getCurrentParticipants() > 0) {
            return HttpResult.error(400, "该课程已有学员报名，无法删除");
        }

        boolean success = this.removeById(courseId);
        if (!success) {
            return HttpResult.error(500, "删除课程失败");
        }

        // ✅ 删除关联的培训活动
        if (relatedActivity != null) {
            activityService.removeById(relatedActivity.getId());
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

        int deletedRows = courseMapper.deleteHighlightsByCourseId(courseId);
        log.info("删除关联: courseId={}, deletedRows={}", courseId, deletedRows);

        if (course.getHighlights() != null && !course.getHighlights().isEmpty()) {
            int successCount = 0;
            for (Highlight highlight : course.getHighlights()) {
                int inserted = courseMapper.insertCourseHighlight(courseId, highlight.getId());
                if (inserted > 0) {
                    successCount++;
                }
            }
            log.info("新增关联: courseId={}, successCount={}", courseId, successCount);
        }
    }

    // ========== 新增：培训活动管理 ==========

    /**
     * 为课程创建对应的培训活动
     */
    private void createTrainingActivity(Course course) {
        try {
            Activity activity = new Activity();
            activity.setActivityName(course.getCourseName());
            activity.setActivityType("培训");
            activity.setDescription(course.getDescription());

            // ✅ 修改：使用固定的默认时间，避免解析错误
            // 培训课程是长期固定的，不需要具体的开始时间
            activity.setStartTime(LocalDateTime.now().plusYears(10)); // 设置一个很远的未来时间，表示长期有效

            activity.setLocation(course.getLocation() != null && !course.getLocation().isEmpty()
                    ? course.getLocation()
                    : "待定");
            activity.setMaxParticipants(course.getMaxStudents() != null ? course.getMaxStudents() : 0);
            activity.setCurrentParticipants(0);
            activity.setContactPerson(course.getInstructor() != null
                    ? course.getInstructor()
                    : "专业教练");
            activity.setContactPhone(course.getContactPhone() != null && !course.getContactPhone().isEmpty()
                    ? course.getContactPhone()
                    : null);
            activity.setCourseId(course.getId());

            activityService.save(activity);
            log.info("✅ 为课程【{}】创建了对应的培训活动，活动ID: {}",
                    course.getCourseName(), activity.getId());
        } catch (Exception e) {
            log.error("❌ 创建培训活动失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 更新课程时同步更新培训活动
     */
    private void updateTrainingActivity(Course course) {
        try {
            Activity activity = activityService.lambdaQuery()
                    .eq(Activity::getCourseId, course.getId())
                    .one();

            if (activity != null) {
                activity.setActivityName(course.getCourseName());
                activity.setDescription(course.getDescription());
                // ✅ 修改：更新时也使用固定时间
                activity.setStartTime(LocalDateTime.now().plusYears(10));
                activity.setLocation(course.getLocation() != null && !course.getLocation().isEmpty()
                        ? course.getLocation()
                        : "待定");
                activity.setContactPerson(course.getInstructor() != null
                        ? course.getInstructor()
                        : "专业教练");
                activity.setMaxParticipants(course.getMaxStudents() != null ? course.getMaxStudents() : 0);
                activity.setContactPhone(course.getContactPhone() != null && !course.getContactPhone().isEmpty()
                        ? course.getContactPhone()
                        : null);

                activityService.updateById(activity);
                log.info("✅ 同步更新了课程【{}】对应的培训活动", course.getCourseName());
            }
        } catch (Exception e) {
            log.error("❌ 更新培训活动失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 解析上课时间
     * 示例：
     * - "每周三 19:00-21:00" -> 返回下一个周三 19:00
     * - "2024-12-25 14:00" -> 返回指定时间
     * - null/空 -> 返回一周后
     */
    private LocalDateTime parseScheduleTime(String scheduleTime) {
        if (scheduleTime == null || scheduleTime.isEmpty()) {
            return LocalDateTime.now().plusDays(7); // 默认一周后
        }

        try {
            // 处理 "每周X" 格式
            if (scheduleTime.contains("每周")) {
                return parseWeeklySchedule(scheduleTime);
            }

            // 处理 ISO 格式日期时间
            if (scheduleTime.contains("T") || scheduleTime.matches("\\d{4}-\\d{2}-\\d{2}.*")) {
                return LocalDateTime.parse(scheduleTime);
            }

            // 默认返回一周后
            return LocalDateTime.now().plusDays(7);
        } catch (Exception e) {
            log.warn("⚠️ 解析上课时间失败: {}, 使用默认值", scheduleTime);
            return LocalDateTime.now().plusDays(7);
        }
    }

    /**
     * 解析周期性课程时间
     * 例如："每周三 19:00-21:00" -> 下一个周三 19:00
     */
    private LocalDateTime parseWeeklySchedule(String scheduleTime) {
        try {
            // 提取星期几
            DayOfWeek dayOfWeek = extractDayOfWeek(scheduleTime);

            // 提取时间（格式：HH:mm）
            LocalTime time = extractTime(scheduleTime);

            // 计算下一个指定星期的日期
            LocalDateTime nextClass = LocalDateTime.now()
                    .with(TemporalAdjusters.next(dayOfWeek))
                    .with(time);

            return nextClass;
        } catch (Exception e) {
            log.warn("⚠️ 解析周期性时间失败: {}", scheduleTime);
            return LocalDateTime.now().plusDays(7);
        }
    }

    /**
     * 从文本中提取星期几
     */
    private DayOfWeek extractDayOfWeek(String text) {
        if (text.contains("一")) return DayOfWeek.MONDAY;
        if (text.contains("二")) return DayOfWeek.TUESDAY;
        if (text.contains("三")) return DayOfWeek.WEDNESDAY;
        if (text.contains("四")) return DayOfWeek.THURSDAY;
        if (text.contains("五")) return DayOfWeek.FRIDAY;
        if (text.contains("六")) return DayOfWeek.SATURDAY;
        if (text.contains("日") || text.contains("天")) return DayOfWeek.SUNDAY;
        return DayOfWeek.MONDAY; // 默认周一
    }

    /**
     * 从文本中提取时间
     * 例如："19:00-21:00" -> 19:00
     */
    private LocalTime extractTime(String text) {
        try {
            // 匹配 HH:mm 格式
            String[] parts = text.split("[-\\s]+");
            for (String part : parts) {
                if (part.matches("\\d{1,2}:\\d{2}")) {
                    return LocalTime.parse(part);
                }
            }
        } catch (Exception e) {
            log.warn("⚠️ 提取时间失败: {}", text);
        }
        return LocalTime.of(19, 0); // 默认 19:00
    }

}