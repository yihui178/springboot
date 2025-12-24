package com.example.mybatis.controller;

import com.example.mybatis.common.HttpResult;
import com.example.mybatis.dto.ActivityDTO;
import com.example.mybatis.service.ActivityService;
import com.github.pagehelper.PageInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 活动管理控制器
 * @author yihui
 */
@RestController
@RequestMapping("/activity")
@RequiredArgsConstructor
@Tag(name = "活动管理", description = "活动的增删改查与分页接口")
public class ActivityController {

    private final ActivityService activityService;

    /**
     * 分页查询活动
     */
    @GetMapping("/page")
    @Operation(summary = "分页查询活动")
    public HttpResult<PageInfo<ActivityDTO>> page(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword) {
        return HttpResult.ok(activityService.pageActivitiesWithDTO(page, pageSize, keyword));
    }

    /**
     * 新增活动
     */
    @PostMapping("/add")
    @Operation(summary = "新增活动")
    public HttpResult<String> add(@Valid @RequestBody ActivityDTO dto) {
        return activityService.addActivity(dto);
    }

    /**
     * 更新活动
     */
    @PutMapping("/update")
    @Operation(summary = "更新活动")
    public HttpResult<String> update(@Valid @RequestBody ActivityDTO dto) {
        return activityService.updateActivity(dto);
    }

    /**
     * 删除活动
     */
    @PostMapping("/delete")
    @Operation(summary = "删除活动")
    public HttpResult<String> delete(@RequestBody ActivityDTO dto) {
        return activityService.deleteActivity(dto.getId());
    }
}