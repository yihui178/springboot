package com.example.mybatis.service.impl;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.mybatis.common.HttpResult;
import com.example.mybatis.dto.ActivityEnrollmentDTO;
import com.example.mybatis.entity.Activity;
import com.example.mybatis.entity.ActivityEnrollment;
import com.example.mybatis.entity.Role;
import com.example.mybatis.entity.UserRole;
import com.example.mybatis.mapper.ActivityEnrollmentMapper;
import com.example.mybatis.service.*;
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
    private final NotificationService notificationService;
    private final RoleService roleService;
    private final UserRoleService userRoleService;
    // ========== 统计已通过的报名人数 ==========
    private long countApprovedEnrollments(Long activityId) {
        return this.lambdaQuery()
                .eq(ActivityEnrollment::getActivityId, activityId)
                .eq(ActivityEnrollment::getEnrollmentStatus, "APPROVED")
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
    // 向所有管理员发送通知
    private void notifyAllAdmins(String title, String content, String type) {
        try {
            // 1. 查询管理员角色（admin 和 super）
            List<Role> adminRoles = roleService.lambdaQuery()
                    .in(Role::getCode, "admin", "super")
                    .list();
            if (adminRoles.isEmpty()) {
                log.warn("⚠️ 未找到管理员角色");
                return;
            }
            // 2. 获取管理员角色的 ID 列表
            List<Long> adminRoleIds = adminRoles.stream()
                    .map(Role::getId)
                    .collect(Collectors.toList());
            // 3. 查询所有管理员的 userId
            List<Long> adminUserIds = userRoleService.lambdaQuery()
                    .in(UserRole::getRoleId, adminRoleIds)
                    .list()
                    .stream()
                    .map(UserRole::getUserId)
                    .distinct()
                    .collect(Collectors.toList());
            if (adminUserIds.isEmpty()) {
                log.warn("⚠️ 未找到管理员用户");
                return;
            }
            // 4. 批量发送通知
            int successCount = 0;
            for (Long adminUserId : adminUserIds) {
                try {
                    notificationService.sendNotification(
                            adminUserId,
                            title,
                            content,
                            type
                    );
                    successCount++;
                } catch (Exception e) {
                    log.error("❌ 向管理员 {} 发送通知失败", adminUserId, e);
                }
            }

        } catch (Exception e) {
            log.error("❌ 批量发送通知给管理员失败", e);
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
        // 2. 改进的重复报名检查
        ActivityEnrollment existing = null;
            // 通过手机号检查
        existing = this.lambdaQuery()
                .eq(ActivityEnrollment::getActivityId, dto.getActivityId())
                .eq(ActivityEnrollment::getMemberPhone, dto.getMemberPhone())
                .in(ActivityEnrollment::getEnrollmentStatus, "PENDING", "APPROVED")
                .one();
        if (existing != null) {
            // 更友好的提示信息
            String status = existing.getEnrollmentStatus();
            if ("PENDING".equals(status)) {
                return HttpResult.error(400, "您已报名该活动，正在等待审核");
            } else if ("APPROVED".equals(status)) {
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
        enrollment.setEnrollmentStatus("PENDING");
        enrollment.setEnrollmentTime(LocalDateTime.now());

        boolean success = this.save(enrollment);
        if (!success) {
            return HttpResult.error(500, "报名失败");
        }
        // 5. 通知管理员
        notifyAllAdmins(
                "新的活动报名",
                String.format("%s 报名了活动【%s】，请及时审核",
                        dto.getMemberName(),
                        activity.getActivityName()),
                "ACTIVITY"
        );
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
        // 发送审核结果通知给报名者
        if (enrollment.getMemberId() != null) {
            try {
                String title = status.equals("APPROVED") ? "报名审核通过" : "报名审核未通过";
                String content = String.format(
                        "您报名的活动【%s】%s",
                        activity != null ? activity.getActivityName() : "未知活动",
                        status.equals("APPROVED")
                                ? "已通过审核，请按时参加"
                                : "未通过审核"
                );
                notificationService.sendNotification(
                        enrollment.getMemberId(),
                        title,
                        content,
                        "ACTIVITY"
                );

            } catch (Exception e) {
                log.error("❌ 发送审核通知失败", e);
            }
        }
        String message = status.equals("APPROVED") ? "审核通过" : "审核拒绝";
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
    // ========== 私有辅助方法 ==========
    private ActivityEnrollmentDTO convertToDTO(ActivityEnrollment enrollment) {
        ActivityEnrollmentDTO dto = new ActivityEnrollmentDTO();
        BeanUtils.copyProperties(enrollment, dto);
        return dto;
    }
}