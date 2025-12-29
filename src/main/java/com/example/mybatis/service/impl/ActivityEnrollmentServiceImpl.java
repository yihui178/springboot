package com.example.mybatis.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.mybatis.common.HttpResult;
import com.example.mybatis.dto.ActivityEnrollmentDTO;
import com.example.mybatis.entity.Activity;
import com.example.mybatis.entity.ActivityEnrollment;
import com.example.mybatis.mapper.ActivityEnrollmentMapper;
import com.example.mybatis.service.*;
import com.example.mybatis.utils.AdminNotificationUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author yihui
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ActivityEnrollmentServiceImpl
        extends ServiceImpl<ActivityEnrollmentMapper, ActivityEnrollment>
        implements ActivityEnrollmentService {

    private final ActivityEnrollmentMapper enrollmentMapper;
    private final ActivityService activityService;
    private final NotificationService notificationService;
    private final AdminNotificationUtil adminNotificationUtil;

    // 常量定义
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_REJECTED = "REJECTED";
    private static final String NOTIFICATION_TYPE_ACTIVITY = "ACTIVITY";

    // ========== 统计已通过的报名人数 ==========
    private long countApprovedEnrollments(Long activityId) {
        return this.lambdaQuery()
                .eq(ActivityEnrollment::getActivityId, activityId)
                .eq(ActivityEnrollment::getEnrollmentStatus, STATUS_APPROVED)
                .count();
    }

    // ========== 更新活动报名人数 ==========
    private void updateActivityParticipants(Long activityId) {
        Activity activity = activityService.getById(activityId);
        if (activity != null) {
            long approvedCount = countApprovedEnrollments(activityId);
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

        // 2. 检查重复报名
        ActivityEnrollment existing = this.lambdaQuery()
                .eq(ActivityEnrollment::getActivityId, dto.getActivityId())
                .eq(ActivityEnrollment::getMemberPhone, dto.getMemberPhone())
                .in(ActivityEnrollment::getEnrollmentStatus, STATUS_PENDING, STATUS_APPROVED)
                .one();

        if (existing != null) {
            String status = existing.getEnrollmentStatus();
            if (STATUS_PENDING.equals(status)) {
                return HttpResult.error(400, "您已报名该活动，正在等待审核");
            } else if (STATUS_APPROVED.equals(status)) {
                return HttpResult.error(400, "您已成功报名该活动，无需重复报名");
            }
        }

        // 3. 检查人数限制
        long approvedCount = countApprovedEnrollments(dto.getActivityId());
        if (activity.getMaxParticipants() > 0 && approvedCount >= activity.getMaxParticipants()) {
            return HttpResult.error(400, "活动报名人数已满");
        }

        // 4. 创建报名记录
        ActivityEnrollment enrollment = new ActivityEnrollment();
        BeanUtils.copyProperties(dto, enrollment);
        enrollment.setEnrollmentStatus(STATUS_PENDING);
        enrollment.setEnrollmentTime(LocalDateTime.now());

        boolean success = this.save(enrollment);
        if (!success) {
            return HttpResult.error(500, "报名失败");
        }

        // 5. 使用工具类通知管理员
        adminNotificationUtil.notifyAllAdmins(
                "新的活动报名",
                String.format("%s 报名了活动【%s】，请及时审核",
                        dto.getMemberName(),
                        activity.getActivityName()),
                NOTIFICATION_TYPE_ACTIVITY
        );

        return HttpResult.ok("报名成功，请等待审核");
    }

    // ========== 审核操作 ==========
    @Override
    @Transactional(rollbackFor = Exception.class)
    public HttpResult<String> reviewEnrollment(Long enrollmentId, String status) {
        // 1. 校验参数
        if (!STATUS_APPROVED.equals(status) && !STATUS_REJECTED.equals(status)) {
            return HttpResult.error(400, "状态参数错误");
        }

        // 2. 查询报名记录
        ActivityEnrollment enrollment = this.getById(enrollmentId);
        if (enrollment == null) {
            return HttpResult.error(404, "报名记录不存在");
        }

        // 3. 检查当前状态
        if (!STATUS_PENDING.equals(enrollment.getEnrollmentStatus())) {
            return HttpResult.error(400, "该报名已审核，无需重复操作");
        }

        // 4. 获取活动信息
        Activity activity = activityService.getById(enrollment.getActivityId());

        // 5. 更新状态
        enrollment.setEnrollmentStatus(status);
        boolean success = this.updateById(enrollment);
        if (!success) {
            return HttpResult.error(500, "审核失败");
        }

        // 6. 更新活动报名人数
        updateActivityParticipants(enrollment.getActivityId());

        // 7. 发送审核结果通知给报名者
        if (enrollment.getMemberId() != null) {
            try {
                String title = STATUS_APPROVED.equals(status) ? "报名审核通过" : "报名审核未通过";
                String content = String.format(
                        "您报名的活动【%s】%s",
                        activity != null ? activity.getActivityName() : "未知活动",
                        STATUS_APPROVED.equals(status)
                                ? "已通过审核，请按时参加"
                                : "未通过审核"
                );
                notificationService.sendNotification(
                        enrollment.getMemberId(),
                        title,
                        content,
                        NOTIFICATION_TYPE_ACTIVITY
                );
            } catch (Exception e) {
                log.error("❌ 发送审核通知失败", e);
            }
        }

        String message = STATUS_APPROVED.equals(status) ? "审核通过" : "审核拒绝";
        return HttpResult.ok(message);
    }

    // ========== 查询操作 ==========
    @Override
    public List<ActivityEnrollmentDTO> getEnrollmentsByActivityId(Long activityId) {
        return enrollmentMapper.selectByActivityId(activityId);
    }

    @Override
    public List<ActivityEnrollmentDTO> getEnrollmentsByMemberId(Long memberId) {
        return enrollmentMapper.selectByMemberId(memberId);
    }
}