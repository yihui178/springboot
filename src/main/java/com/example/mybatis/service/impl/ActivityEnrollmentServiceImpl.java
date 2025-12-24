package com.example.mybatis.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.mybatis.common.HttpResult;
import com.example.mybatis.dto.ActivityEnrollmentDTO;
import com.example.mybatis.entity.Activity;
import com.example.mybatis.entity.ActivityEnrollment;
import com.example.mybatis.mapper.ActivityEnrollmentMapper;
import com.example.mybatis.service.ActivityEnrollmentService;
import com.example.mybatis.service.ActivityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ActivityEnrollmentServiceImpl
        extends ServiceImpl<ActivityEnrollmentMapper, ActivityEnrollment>
        implements ActivityEnrollmentService {

    private final ActivityEnrollmentMapper enrollmentMapper;
    private final ActivityService activityService;

    // ========== ✅ 统计已通过的报名人数 ==========
    private long countApprovedEnrollments(Long activityId) {
        return this.lambdaQuery()
                .eq(ActivityEnrollment::getActivityId, activityId)
                .eq(ActivityEnrollment::getEnrollmentStatus, "APPROVED")
                .count();
    }

    // ========== ✅ 更新活动报名人数 ==========
    private void updateActivityParticipants(Long activityId) {
        Activity activity = activityService.getById(activityId);
        if (activity != null) {
            long approvedCount = countApprovedEnrollments(activityId);
            // ✅ 安全转换为 int
            activity.setCurrentParticipants((int) approvedCount);
            activityService.updateById(activity);
            log.info("更新活动报名人数: activityId={}, count={}", activityId, approvedCount);
        }
    }

    // ========== 报名操作 ==========
    @Override
    @Transactional(rollbackFor = Exception.class)
    public HttpResult<String> enrollActivity(ActivityEnrollmentDTO dto) {
        // 1. 校验活动是否存在
        Activity activity = activityService.getById(dto.getActivityId());
        if (activity == null) {
            return HttpResult.error(404, "活动不存在");
        }

        // 2. 检查是否已报名
        ActivityEnrollment existing = this.lambdaQuery()
                .eq(ActivityEnrollment::getActivityId, dto.getActivityId())
                .eq(ActivityEnrollment::getMemberId, dto.getMemberId())
                .one();

        if (existing != null) {
            return HttpResult.error(400, "您已报名该活动，请勿重复报名");
        }

        // 3. 检查人数限制（✅ 只统计已通过的）
        long approvedCount = countApprovedEnrollments(dto.getActivityId());
        if (activity.getMaxParticipants() > 0 && approvedCount >= activity.getMaxParticipants()) {
            return HttpResult.error(400, "活动报名人数已满");
        }

        // 4. 创建报名记录
        ActivityEnrollment enrollment = new ActivityEnrollment();
        BeanUtils.copyProperties(dto, enrollment);
        enrollment.setEnrollmentStatus("PENDING");
        enrollment.setEnrollmentTime(LocalDateTime.now());

        boolean success = this.save(enrollment);
        if (!success) {
            return HttpResult.error(500, "报名失败");
        }

        return HttpResult.ok("报名成功，请等待审核");
    }

    // ========== 审核操作 ==========
    @Override
    @Transactional(rollbackFor = Exception.class)
    public HttpResult<String> reviewEnrollment(Long enrollmentId, String status) {
        // 1. 校验参数
        if (!status.equals("APPROVED") && !status.equals("REJECTED")) {
            return HttpResult.error(400, "状态参数错误");
        }

        // 2. 查询报名记录
        ActivityEnrollment enrollment = this.getById(enrollmentId);
        if (enrollment == null) {
            return HttpResult.error(404, "报名记录不存在");
        }

        // 3. 检查当前状态
        if (!enrollment.getEnrollmentStatus().equals("PENDING")) {
            return HttpResult.error(400, "该报名已审核，无需重复操作");
        }

        // 4. 更新状态
        enrollment.setEnrollmentStatus(status);
        boolean success = this.updateById(enrollment);

        if (!success) {
            return HttpResult.error(500, "审核失败");
        }

        // ✅ 审核后更新活动报名人数
        updateActivityParticipants(enrollment.getActivityId());

        String message = status.equals("APPROVED") ? "审核通过" : "审核拒绝";
        return HttpResult.ok(message);
    }

    // ========== 取消报名 ==========
    @Override
    @Transactional(rollbackFor = Exception.class)
    public HttpResult<String> cancelEnrollment(Long enrollmentId) {
        // 1. 查询报名记录
        ActivityEnrollment enrollment = this.getById(enrollmentId);
        if (enrollment == null) {
            return HttpResult.error(404, "报名记录不存在");
        }

        // 2. 删除报名记录
        boolean success = this.removeById(enrollmentId);
        if (!success) {
            return HttpResult.error(500, "取消报名失败");
        }

        // ✅ 取消后更新活动报名人数
        updateActivityParticipants(enrollment.getActivityId());

        return HttpResult.ok("取消报名成功");
    }

    // ========== 查询操作 ==========
    @Override
    public List<ActivityEnrollmentDTO> getEnrollmentsByActivityId(Long activityId) {
        // 直接返回，无需转换
        return enrollmentMapper.selectByActivityId(activityId);
    }
    @Override
    public List<ActivityEnrollmentDTO> getEnrollmentsByMemberId(Long memberId) {
        // 直接返回，无需转换
        return enrollmentMapper.selectByMemberId(memberId);
    }
    // ========== 私有辅助方法 ==========
    private ActivityEnrollmentDTO convertToDTO(ActivityEnrollment enrollment) {
        ActivityEnrollmentDTO dto = new ActivityEnrollmentDTO();
        BeanUtils.copyProperties(enrollment, dto);
        return dto;
    }
}