package com.example.mybatis.controller;
import com.example.mybatis.common.HttpResult;
import com.example.mybatis.dto.CourseDTO;
import com.example.mybatis.service.CourseService;
import com.github.pagehelper.PageInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 课程管理控制器（精简版）
 * @author yihui
 */
@RestController
@RequestMapping("/course")
@RequiredArgsConstructor
@Tag(name = "课程管理", description = "课程的增删改查与分页接口")
public class CourseController {

    private final CourseService courseService;

    /**
     * 分页查询课程
     */
    @GetMapping("/page")
    @Operation(summary = "分页查询课程")
    public HttpResult<PageInfo<CourseDTO>> page(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "5") Integer pageSize,
            @RequestParam(required = false) String keyword) {
        return HttpResult.ok(courseService.pageCoursesWithDTO(page, pageSize, keyword));
    }
    /**
     * 新增课程
     */
    @PostMapping("/add")
    @Operation(summary = "新增课程")
    public HttpResult<String> add(@Valid @RequestBody CourseDTO dto) {
        return courseService.addCourse(dto);
    }
    /**
     * 更新课程
     */
    @PutMapping("/update")
    @Operation(summary = "更新课程")
    public HttpResult<String> update(@Valid @RequestBody CourseDTO dto) {
        return courseService.updateCourse(dto);
    }
    /**
     * 删除课程
     */
    @PostMapping("/delete")
    @Operation(summary = "删除课程")
    public HttpResult<String> delete(@RequestBody CourseDTO dto) {
        return courseService.deleteCourse(dto.getId());
    }
}