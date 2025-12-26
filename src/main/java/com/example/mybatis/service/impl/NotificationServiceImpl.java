package com.example.mybatis.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.mybatis.common.HttpResult;
import com.example.mybatis.dto.NotificationDTO;
import com.example.mybatis.entity.Notification;
import com.example.mybatis.mapper.NotificationMapper;
import com.example.mybatis.service.NotificationService;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 通知服务实现类
 * @author yihui
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl extends ServiceImpl<NotificationMapper, Notification>
        implements NotificationService {

    private final NotificationMapper notificationMapper;
    private final ApplicationContext applicationContext;

    /**
     * 分页查询用户通知
     */
    @Override
    public PageInfo<NotificationDTO> pageNotifications(Long userId, int page, int pageSize) {
        PageHelper.startPage(page, pageSize);
        List<Notification> list = this.lambdaQuery()
                .eq(Notification::getUserId, userId)
                .orderByDesc(Notification::getCreateTime)
                .list();

        PageInfo<Notification> pageInfo = new PageInfo<>(list);
        PageInfo<NotificationDTO> dtoPage = new PageInfo<>();

        dtoPage.setList(list.stream().map(this::toDTO).collect(Collectors.toList()));
        dtoPage.setPageNum(pageInfo.getPageNum());
        dtoPage.setPageSize(pageInfo.getPageSize());
        dtoPage.setTotal(pageInfo.getTotal());
        dtoPage.setPages(pageInfo.getPages());

        return dtoPage;
    }

    /**
     * 标记为已读
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public HttpResult<String> markAsRead(Long notificationId, Long userId) {
        Notification notification = this.getById(notificationId);
        if (notification == null || !notification.getUserId().equals(userId)) {
            return HttpResult.error(404, "通知不存在或无权操作");
        }

        notification.setIsRead(true);
        this.updateById(notification);
        return HttpResult.ok("已标记为已读");
    }

    /**
     * 全部标记为已读
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public HttpResult<String> markAllAsRead(Long userId) {
        this.lambdaUpdate()
                .eq(Notification::getUserId, userId)
                .eq(Notification::getIsRead, false)
                .set(Notification::getIsRead, true)
                .update();
        return HttpResult.ok("全部已标记为已读");
    }

    /**
     * 获取未读数量
     */
    @Override
    public int getUnreadCount(Long userId) {
        return notificationMapper.countUnreadByUserId(userId);
    }

    /**
     * 发送通知
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sendNotification(Long userId, String title, String content, String type) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setType(type);
        notification.setIsRead(false);
        this.save(notification);

        try {
            applicationContext.getBean(com.example.mybatis.controller.NotificationController.class)
                    .sendToUser(userId, toDTO(notification));
        } catch (Exception e) {
            log.warn("SSE 推送失败: userId={}", userId);
        }
    }

    /**
     * 清空当前用户所有通知
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public HttpResult<String> clearAllByUserId(Long userId) {
        this.lambdaUpdate().eq(Notification::getUserId, userId).remove();
        return HttpResult.ok("清空成功");
    }

    /**
     * Entity 转 DTO
     */
    private NotificationDTO toDTO(Notification notification) {
        NotificationDTO dto = new NotificationDTO();
        BeanUtils.copyProperties(notification, dto);
        return dto;
    }
}