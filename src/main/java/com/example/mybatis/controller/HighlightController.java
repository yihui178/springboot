package com.example.mybatis.controller;

import com.example.mybatis.common.HttpResult;
import com.example.mybatis.dto.HighlightDTO;
import com.example.mybatis.entity.Highlight;
import com.example.mybatis.mapper.HighlightMapper;
import com.example.mybatis.service.HighlightService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/highlight")
@Tag(name = "亮点管理", description = "亮点的增删改查与分页接口")
public class HighlightController {

    @Autowired
    private HighlightService highlightService;

    @Autowired
    private HighlightMapper highlightMapper; // 🔥 注入 Mapper

    @GetMapping("/list")
    @Operation(summary = "查询所有亮点")
    public HttpResult<?> list() {
        return HttpResult.ok(highlightService.list());
    }

    @PostMapping("/add")
    @Operation(summary = "新增亮点")
    public HttpResult<?> add(@Valid @RequestBody HighlightDTO dto) {
        Highlight highlight = new Highlight();
        BeanUtils.copyProperties(dto, highlight);
        return highlightService.save(highlight)
                ? HttpResult.ok("新增成功")
                : HttpResult.error(500, "新增失败");
    }

    @PutMapping("/update")
    @Operation(summary = "更新亮点")
    public HttpResult<?> update(@Valid @RequestBody HighlightDTO dto) {
        if (dto.getId() == null) {
            return HttpResult.error(400, "ID不能为空");
        }
        Highlight highlight = new Highlight();
        BeanUtils.copyProperties(dto, highlight);
        return highlightService.updateById(highlight)
                ? HttpResult.ok("更新成功")
                : HttpResult.error(500, "更新失败");
    }

    @PostMapping("/delete")
    @Operation(summary = "删除亮点")
    public HttpResult<?> delete(@RequestBody HighlightDTO dto) {
        if (dto.getId() == null) {
            return HttpResult.error(400, "ID不能为空");
        }

        // 🔥 检查是否有课程引用
        int count = highlightMapper.countCoursesByHighlightId(dto.getId());
        if (count > 0) {
            return HttpResult.error(400, "该亮点已被 " + count + " 个课程引用，无法删除");
        }

        return highlightService.removeById(dto.getId())
                ? HttpResult.ok("删除成功")
                : HttpResult.error(500, "删除失败");
    }
}