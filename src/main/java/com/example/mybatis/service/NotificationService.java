package com.example.mybatis.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.mybatis.common.HttpResult;
import com.example.mybatis.dto.NotificationDTO;
import com.example.mybatis.entity.Notification;
import com.github.pagehelper.PageInfo;

public interface NotificationService extends IService<Notification> {
    /**
     * 分页查询用户通知
     */
    PageInfo<NotificationDTO> pageNotifications(Long userId, int page, int pageSize);

    /**
     * 标记为已读
     */
    HttpResult<String> markAsRead(Long notificationId, Long userId);

    /**
     * 全部标记为已读
     */
    HttpResult<String> markAllAsRead(Long userId);

    /**
     * 清空当前用户所有消息
     */
    HttpResult<String> clearAllByUserId(Long userId);

    /**
     * 获取未读数量
     */
    int getUnreadCount(Long userId);

    /**
     * 发送通知
     */
    void sendNotification(Long userId, String title, String content, String type);


}