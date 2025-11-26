package com.example.mybatis.controller;
import com.example.mybatis.common.HttpResult;
import com.example.mybatis.dto.HighlightDTO;
import com.example.mybatis.service.HighlightService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.List;
/**
 * 亮点管理控制器（优化版）
 * 职责：仅负责接收请求和返回响应
 * @author yihui
 */
@RestController
@RequestMapping("/highlight")
@Tag(name = "亮点管理", description = "亮点的增删改查接口")
public class HighlightController {

    private final HighlightService highlightService;

    public HighlightController(HighlightService highlightService) {
        this.highlightService = highlightService;
    }
    /**
     * 查询所有亮点
     */
    @GetMapping("/list")
    @Operation(summary = "查询所有亮点")
    public HttpResult<List<HighlightDTO>> list() {
        return HttpResult.ok(highlightService.listAllHighlights());
    }
    /**
     * 新增亮点
     */
    @PostMapping("/add")
    @Operation(summary = "新增亮点")
    public HttpResult<String> add(@Valid @RequestBody HighlightDTO dto) {
        highlightService.addHighlight(dto);
        return HttpResult.ok("新增成功");
    }
    /**
     * 更新亮点
     */
    @PutMapping("/update")
    @Operation(summary = "更新亮点")
    public HttpResult<String> update(@Valid @RequestBody HighlightDTO dto) {
        highlightService.updateHighlight(dto);
        return HttpResult.ok("更新成功");
    }
    /**
     * 删除亮点
     */
    @PostMapping("/delete")
    @Operation(summary = "删除亮点")
    public HttpResult<String> delete(@RequestBody HighlightDTO dto) {
        highlightService.deleteHighlight(dto.getId());
        return HttpResult.ok("删除成功");
    }
}