package com.example.mybatis.controller;
import com.example.mybatis.common.HttpResult;
import com.example.mybatis.dto.NotificationDTO;
import com.example.mybatis.service.NotificationService;
import com.example.mybatis.utils.JwtUtils;
import com.example.mybatis.utils.RequestUtils;
import com.github.pagehelper.PageInfo;
import io.jsonwebtoken.Claims;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
/**
 * 通知管理控制器
 * @author yihui
 */
@Slf4j
@RestController
@RequestMapping("/notification")
@RequiredArgsConstructor
@Tag(name = "通知管理")
public class NotificationController {
    private final NotificationService notificationService;
    private final JwtUtils jwtUtils;
    private final Map<Long, SseEmitter> sseEmitters = new ConcurrentHashMap<>();
    /**
     * 建立 SSE 连接（支持 token 参数）
     */
    @GetMapping(value = "/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "建立 SSE 连接")
    public SseEmitter subscribe(@RequestParam(required = false) String token) {

        // 手动验证 token 并提取 userId
        Long userId = null;
        if (token != null && !token.isEmpty()) {
            try {
                Claims claims = jwtUtils.parseClaims(token);
                userId = claims.get("userId", Long.class);

            } catch (Exception e) {
                throw new RuntimeException("Token 无效或已过期");
            }
        } else {
            throw new RuntimeException("缺少 token 参数");
        }
        // 创建 SSE 连接
        SseEmitter emitter = new SseEmitter(1800000L); // 30分钟超时
        final Long finalUserId = userId;
        // 保存连接
        sseEmitters.put(finalUserId, emitter);
        // 连接关闭时清理
        emitter.onCompletion(() -> {
            sseEmitters.remove(finalUserId);
        });
        emitter.onTimeout(() -> {
            sseEmitters.remove(finalUserId);
        });
        emitter.onError(throwable -> {
            sseEmitters.remove(finalUserId);
        });
        // 发送初始化消息
        try {
            emitter.send(SseEmitter.event().name("connect").data("连接成功"));
        } catch (IOException e) {
            sseEmitters.remove(finalUserId);
        }
        return emitter;
    }
    /**
     * 移除用户的 SSE 连接（登出时调用）
     */
    public void removeConnection(Long userId) {
        SseEmitter emitter = sseEmitters.remove(userId);
        if (emitter != null) {
            try {
                emitter.complete();

            } catch (Exception e) {
            }
        }
    }
    /**
     * 向指定用户推送通知（内部方法）
     */
    public void sendToUser(Long userId, NotificationDTO notification) {
        SseEmitter emitter = sseEmitters.get(userId);
        if (emitter == null) {
            return;
        }
        try {
            emitter.send(SseEmitter.event().name("notification").data(notification));

        } catch (IOException e) {
            sseEmitters.remove(userId);
        }
    }
    /**
     * 分页查询通知
     */
    @GetMapping("/page")
    @Operation(summary = "分页查询通知")
    public HttpResult<PageInfo<NotificationDTO>> page(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            HttpServletRequest request) {
        return HttpResult.ok(notificationService.pageNotifications(
                RequestUtils.getCurrentUserId(request), page, pageSize));
    }
    /**
     * 获取未读数量
     */
    @GetMapping("/unread-count")
    @Operation(summary = "获取未读数量")
    public HttpResult<Integer> getUnreadCount(HttpServletRequest request) {
        return HttpResult.ok(notificationService.getUnreadCount(
                RequestUtils.getCurrentUserId(request)));
    }
    /**
     * 标记为已读
     */
    @PostMapping("/mark-read")
    @Operation(summary = "标记为已读")
    public HttpResult<String> markAsRead(@RequestBody Map<String, Long> body,
                                         HttpServletRequest request) {
        return notificationService.markAsRead(body.get("id"),
                RequestUtils.getCurrentUserId(request));
    }
    /**
     * 全部标记为已读
     */
    @PostMapping("/mark-all-read")
    @Operation(summary = "全部标记为已读")
    public HttpResult<String> markAllAsRead(HttpServletRequest request) {
        return notificationService.markAllAsRead(RequestUtils.getCurrentUserId(request));
    }
    /**
     * 清空当前用户的所有通知
     */
    @PostMapping("/clear")
    @Operation(summary = "清空当前用户的所有通知")
    public HttpResult<String> clearAll(HttpServletRequest request) {
        return notificationService.clearAllByUserId(RequestUtils.getCurrentUserId(request));
    }
}