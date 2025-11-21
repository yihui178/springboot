package com.example.mybatis.controller;
import com.example.mybatis.common.HttpResult;
import com.example.mybatis.dto.CourseDTO;
import com.example.mybatis.service.CourseService;
import com.github.pagehelper.PageInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
/**
 * 课程管理控制器（优化版）
 * 职责：仅负责接收请求和返回响应
 */
@RestController
@RequestMapping("/course")
@Tag(name = "课程管理", description = "课程的增删改查与分页接口")
public class CourseController {
    @Autowired
    private CourseService courseService;
    /**
     * 分页查询课程
     */
    @GetMapping("/page")
    @Operation(summary = "分页查询课程")
    public HttpResult<PageInfo<CourseDTO>> page(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "5") Integer pageSize,
            @RequestParam(required = false) String keyword) {

        PageInfo<CourseDTO> pageInfo = courseService.pageCoursesWithDTO(page, pageSize, keyword);
        return HttpResult.ok(pageInfo);
    }
    /**
     * 新增课程
     */
    @PostMapping("/add")
    @Operation(summary = "新增课程")
    public HttpResult<String> add(@Valid @RequestBody CourseDTO dto) {
        courseService.addCourse(dto);
        return HttpResult.ok("新增成功");
    }
    /**
     * 更新课程
     */
    @PutMapping("/update")
    @Operation(summary = "更新课程")
    public HttpResult<String> update(@Valid @RequestBody CourseDTO dto) {
        courseService.updateCourse(dto);
        return HttpResult.ok("更新成功");
    }
    /**
     * 删除课程
     */
    @PostMapping("/delete")
    @Operation(summary = "删除课程")
    public HttpResult<String> delete(@RequestBody CourseDTO dto) {
        courseService.deleteCourse(dto.getId());
        return HttpResult.ok("删除成功");
    }
}