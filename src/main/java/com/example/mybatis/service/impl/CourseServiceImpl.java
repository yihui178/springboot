package com.example.mybatis.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.mybatis.common.HttpResult;
import com.example.mybatis.dto.CourseDTO;
import com.example.mybatis.entity.Course;
import com.example.mybatis.entity.Highlight;
import com.example.mybatis.mapper.CourseMapper;
import com.example.mybatis.service.CourseService;
import com.example.mybatis.service.NotificationService;
import com.example.mybatis.service.MemberService;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CourseServiceImpl extends ServiceImpl<CourseMapper, Course> implements CourseService {

    private final CourseMapper courseMapper;
    private final NotificationService notificationService;
    private final MemberService memberService;

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
            throw new RuntimeException("查询课程分页失败: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public HttpResult<String> addCourse(CourseDTO dto) {
        Course course = convertToEntity(dto);

        boolean success = this.save(course);
        if (!success) {
            return HttpResult.error(500, "新增课程失败");
        }

        saveOrUpdateHighlights(course);

        // 通知所有会员有新课程上线
        try {
            // 获取所有会员ID（简化处理，实际可能需要分批）
            List<Long> memberIds = memberService.list().stream()
                    .map(member -> member.getUserId())
                    .collect(Collectors.toList());

            String content = String.format(
                    "新课程【%s】已上线！%s，%s",
                    course.getCourseName(),
                    course.getCategory(),
                    course.getOnline() ? "线上授课" : "线下实操"
            );

            // 批量发送（实际应该用消息队列）
            for (Long memberId : memberIds) {
                try {
                    notificationService.sendNotification(
                            memberId,
                            "新课程上线通知",
                            content,
                            "COURSE"
                    );
                } catch (Exception e) {
                    log.error("发送通知给会员 {} 失败", memberId, e);
                }
            }
            log.info("已通知 {} 位会员新课程上线", memberIds.size());
        } catch (Exception e) {
            log.error("批量发送课程通知失败", e);
        }

        return HttpResult.ok("新增成功");
    }

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
        BeanUtils.copyProperties( dto,course);
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
}