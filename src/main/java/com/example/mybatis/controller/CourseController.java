package com.example.mybatis.controller;

import com.example.mybatis.dto.CourseDTO;
import com.example.mybatis.entity.Course;
import com.example.mybatis.common.HttpResult;
import com.example.mybatis.entity.Highlight;
import com.example.mybatis.service.impl.CourseServiceImpl;
import com.github.pagehelper.PageInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 课程管理控制器
 * 注：
 * - 所有对外接口使用 DTO（CourseDTO）作为入参/出参（不直接暴露 Entity）
 * - 使用 PageHelper 分页（Service 层执行），Controller 负责转换 DTO
 */
@RestController
@RequestMapping("/course")
@Tag(name = "课程管理", description = "课程的增删改查与分页接口")
public class CourseController {

    @Autowired
    private CourseServiceImpl courseService;

    @GetMapping("/page")
    @Operation(summary = "分页查询课程")
    public HttpResult<?> page(@RequestParam(defaultValue = "1") Integer page,
                              @RequestParam(defaultValue = "5") Integer pageSize,
                              @RequestParam(required = false) String keyword) {

        PageInfo<Course> pi = courseService.pageCourses(page, pageSize, keyword);

        //  边界处理：如果课程列表为空，直接返回空分页结果
        if (pi.getList().isEmpty()) {
            PageInfo<CourseDTO> dtoPage = new PageInfo<>();
            dtoPage.setList(Collections.emptyList());
            dtoPage.setPageNum(pi.getPageNum());
            dtoPage.setPageSize(pi.getPageSize());
            dtoPage.setTotal(pi.getTotal());
            dtoPage.setPages(pi.getPages());
            return HttpResult.ok(dtoPage);
        }
        //  收集所有课程 ID
        List<Long> courseIds = pi.getList().stream()
                .map(Course::getId)
                .collect(Collectors.toList());
        //  批量查询所有课程的亮点（通过 Service 层，只执行 1 次 SQL）
        Map<Long, List<Long>> courseHighlightMap =
                courseService.getHighlightsByCourseIds(courseIds);
        // entity -> dto
        List<CourseDTO> dtoList = pi.getList().stream().map(c -> {
            CourseDTO dto = new CourseDTO();
            BeanUtils.copyProperties(c, dto);
            //  从内存 Map 中获取亮点 ID 列表（不再执行 SQL）
            dto.setHighlightIds(
                    courseHighlightMap.getOrDefault(c.getId(), Collections.emptyList())
            );
            return dto;
        }).collect(Collectors.toList());
        PageInfo<CourseDTO> dtoPage = new PageInfo<>();
        dtoPage.setList(dtoList);
        dtoPage.setPageNum(pi.getPageNum());
        dtoPage.setPageSize(pi.getPageSize());
        dtoPage.setTotal(pi.getTotal());
        dtoPage.setPages(pi.getPages());
        return HttpResult.ok(dtoPage);
    }

    @PostMapping("/add")
    @Operation(summary = "新增课程")
    public HttpResult<?> add(@Valid @RequestBody CourseDTO dto) {
        Course course = convertToEntity(dto);
        return courseService.saveCourse(course)
                ? HttpResult.ok("新增成功")
                : HttpResult.error(500, "保存失败");
    }
    @PutMapping("/update")
    @Operation(summary = "更新课程")
    public HttpResult<?> update(@Valid @RequestBody CourseDTO dto) {
        Course course = convertToEntity(dto);
        return courseService.updateCourse(course)
                ? HttpResult.ok("更新成功")
                : HttpResult.error(500, "更新失败");
    }
    //  提取公共方法
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

    @PostMapping("/delete")
    @Operation(summary = "删除课程")
    public HttpResult<?> delete(@RequestBody CourseDTO dto) {
        if (dto.getId() == null) {return HttpResult.error(400, "ID不能为空");}

        return courseService.removeById(dto.getId()) ?
                HttpResult.ok("删除成功") :
                HttpResult.error(500, "删除失败");
    }
}

