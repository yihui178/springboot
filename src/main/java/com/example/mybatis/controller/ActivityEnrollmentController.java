package com.example.mybatis.controller;

import com.example.mybatis.common.HttpResult;
import com.example.mybatis.dto.ActivityEnrollmentDTO;
import com.example.mybatis.service.ActivityEnrollmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 活动报名控制器
 * @author yihui
 */
@RestController
@RequestMapping("/enrollment")
@RequiredArgsConstructor
@Tag(name = "活动报名", description = "活动报名的增删改查接口")
public class ActivityEnrollmentController {

    private final ActivityEnrollmentService enrollmentService;

    /**
     * 报名活动
     */
    @PostMapping("/enroll")
    @Operation(summary = "报名活动")
    public HttpResult<String> enroll(@Valid @RequestBody ActivityEnrollmentDTO dto) {
        return enrollmentService.enrollActivity(dto);
    }

    /**
     * 审核报名
     */
    @PostMapping("/review")
    @Operation(summary = "审核报名")
    public HttpResult<String> review(@RequestBody Map<String, Object> params) {
        Long enrollmentId = Long.valueOf(params.get("id").toString());
        String status = params.get("status").toString();
        return enrollmentService.reviewEnrollment(enrollmentId, status);
    }

    /**
     * 取消报名
     */
//    @PostMapping("/cancel")
//    @Operation(summary = "取消报名")
//    public HttpResult<String> cancel(@RequestBody Map<String, Long> params) {
//        return enrollmentService.cancelEnrollment(params.get("id"));
//    }

    /**
     * 获取活动的报名列表
     */
    @GetMapping("/list/activity/{activityId}")
    @Operation(summary = "获取活动的报名列表")
    public HttpResult<List<ActivityEnrollmentDTO>> getByActivityId(@PathVariable Long activityId) {
        return HttpResult.ok(enrollmentService.getEnrollmentsByActivityId(activityId));
    }

    /**
     * 获取会员的报名记录
     */
    @GetMapping("/list/member/{memberId}")
    @Operation(summary = "获取会员的报名记录")
    public HttpResult<List<ActivityEnrollmentDTO>> getByMemberId(@PathVariable Long memberId) {
        return HttpResult.ok(enrollmentService.getEnrollmentsByMemberId(memberId));
    }
}