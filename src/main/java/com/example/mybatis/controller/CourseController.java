package com.example.mybatis.controller;

import com.example.mybatis.dto.CourseDTO;
import com.example.mybatis.entity.Course;
import com.example.mybatis.http.HttpResult;
import com.example.mybatis.service.impl.CourseServiceImpl;
import com.github.pagehelper.PageInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 课程管理控制器
 *
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

    /**
     * 分页查询课程
     *
     * @param page     当前页（默认 1）
     * @param pageSize 每页大小（默认 5）
     * @param keyword  可选搜索关键字（课程名或描述）
     * @return PageInfo<CourseDTO>
     */
    @GetMapping("/page")
    @Operation(summary = "分页查询课程", description = "按课程名称或描述模糊查询，返回 PageInfo 封装结果")
    public HttpResult<?> page(@RequestParam(defaultValue = "1") Integer page,
                              @RequestParam(defaultValue = "5") Integer pageSize,
                              @RequestParam(required = false) String keyword) {

        PageInfo<Course> pi = courseService.pageCourses(page, pageSize, keyword);
        // Entity -> DTO
        List<CourseDTO> dtoList = pi.getList().stream().map(c -> {
            CourseDTO dto = new CourseDTO();
            BeanUtils.copyProperties(c, dto);
            if (c.getHighlightStr() != null) {
                dto.setHighlights(List.of(c.getHighlightStr().split(",")));
            }
            return dto;
        }).collect(Collectors.toList());

        // 把 PageInfo 信息映射到新的 PageInfo<CourseDTO>
        com.github.pagehelper.PageInfo<CourseDTO> dtoPage = new com.github.pagehelper.PageInfo<>();
        dtoPage.setList(dtoList);
        dtoPage.setPageNum(pi.getPageNum());
        dtoPage.setPageSize(pi.getPageSize());
        dtoPage.setTotal(pi.getTotal());
        dtoPage.setPages(pi.getPages());

        return HttpResult.ok(dtoPage);
    }

    /**
     * 新增课程
     */
    @PostMapping("/add")
    @Operation(summary = "新增课程")
    public HttpResult<?> add(@RequestBody CourseDTO dto) {
        // DTO -> Entity
        Course course = new Course();
        BeanUtils.copyProperties(dto, course);
        // highlights -> highlightStr（以防前端只传 highlights）
        if (dto.getHighlights() != null && !dto.getHighlights().isEmpty()) {
            course.setHighlightStr(String.join(",", dto.getHighlights()));
        }

        boolean ok = courseService.saveCourse(course);
        return ok ? HttpResult.ok("新增成功") : HttpResult.error(500, "保存失败");
    }

    /**
     * 更新课程
     */
    @PutMapping("/update")
    @Operation(summary = "更新课程")
    public HttpResult<?> update(@RequestBody CourseDTO dto) {
        Course course = new Course();
        BeanUtils.copyProperties(dto, course);

        // highlights -> highlightStr
        if (dto.getHighlights() != null && !dto.getHighlights().isEmpty()) {
            course.setHighlightStr(String.join(",", dto.getHighlights()));
        } else {
            course.setHighlightStr(null);
        }

        boolean ok = courseService.updateCourse(course);
        return ok ? HttpResult.ok("更新成功") : HttpResult.error(500, "更新失败");
    }

    /**
     * 删除课程（按 id）
     */
    @PostMapping("/delete")
    @Operation(summary = "删除课程")
    public HttpResult<?> delete(@RequestBody CourseDTO dto) {
        if (dto.getId() == null) {
            return HttpResult.error(400, "ID不能为空");
        }
        boolean ok = courseService.removeById(dto.getId());
        return ok ? HttpResult.ok("删除成功") : HttpResult.error(500, "删除失败");
    }
}
