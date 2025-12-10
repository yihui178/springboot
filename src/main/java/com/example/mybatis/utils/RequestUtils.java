package com.example.mybatis.utils;
import jakarta.servlet.http.HttpServletRequest;
/**
 * 请求工具类 - 从 Request Attribute 提取用户信息
 * @author yihui
 */
public class RequestUtils {
    /**
     * 从 Request 获取当前用户 ID（严格模式）
     */
    public static Long getCurrentUserId(HttpServletRequest request) {
        Object userIdObj = request.getAttribute("userId");
        if (userIdObj == null) {
            throw new RuntimeException("用户未登录");
        }
        return (Long) userIdObj;
    }
    /**
     * 从 Request 安全获取用户 ID（不抛异常）
     */
    public static Long getCurrentUserIdSafely(HttpServletRequest request) {
        try {
            Object userIdObj = request.getAttribute("userId");
            return userIdObj != null ? (Long) userIdObj : null;
        } catch (Exception e) {
            return null;
        }
    }
    /**
     * 从 Request 获取用户名
     */
    public static String getCurrentUsername(HttpServletRequest request) {
        Object usernameObj = request.getAttribute("username");
        return usernameObj != null ? (String) usernameObj : null;
    }
}