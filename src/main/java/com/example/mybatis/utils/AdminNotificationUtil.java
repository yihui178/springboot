package com.example.mybatis.utils;

import com.example.mybatis.entity.Role;
import com.example.mybatis.entity.UserRole;
import com.example.mybatis.service.NotificationService;
import com.example.mybatis.service.RoleService;
import com.example.mybatis.service.UserRoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 管理员通知工具类
 * @author yihui
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminNotificationUtil {

    private final RoleService roleService;
    private final UserRoleService userRoleService;
    private final NotificationService notificationService;

    /**
     * 批量通知所有管理员
     * @param title 通知标题
     * @param content 通知内容
     * @param type 通知类型（ACTIVITY/COURSE/NEWS/SYSTEM）
     */
    public void notifyAllAdmins(String title, String content, String type) {
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
                    .toList();

            // 3. 查询所有管理员的 userId
            List<Long> adminUserIds = userRoleService.lambdaQuery()
                    .in(UserRole::getRoleId, adminRoleIds)
                    .list()
                    .stream()
                    .map(UserRole::getUserId)
                    .distinct()
                    .toList();

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

            log.info("✅ 已通知 {}/{} 位管理员: {}", successCount, adminUserIds.size(), title);

        } catch (Exception e) {
            log.error("❌ 批量发送通知给管理员失败", e);
        }
    }
}