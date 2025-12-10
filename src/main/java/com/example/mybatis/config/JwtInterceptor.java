package com.example.mybatis.config;
import com.example.mybatis.utils.JwtUtils;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import java.util.List;
/**
 * JWT 拦截器 - 支持双 Token 机制
 * @author yihui
 */
@Slf4j  //添加日志支持
@Component
@RequiredArgsConstructor
public class JwtInterceptor implements HandlerInterceptor {
    private final JwtUtils jwtUtils;
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {
        // 放行 OPTIONS 预检请求
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        // 提取 Access Token
        String accessToken = extractToken(request, "Authorization");

        // 先判断 Token 是否为空
        if (accessToken == null) {
            log.warn("请求缺少 Access Token: {}", request.getRequestURI());
            return unauthorized(response, "Token缺失，请登录");
        }
        try {
            // 解析完整的 Token 信息
            Claims claims = jwtUtils.parseClaims(accessToken);
            Long userId = claims.get("userId", Long.class);
            String username = claims.getSubject();
            @SuppressWarnings("unchecked")
            List<String> roles = claims.get("roles", List.class);
            // 存储更多用户信息（可选）
            request.setAttribute("userId", userId);
            request.setAttribute("username", username);
            request.setAttribute("roles", roles);
            // 添加调试日志
            log.debug("用户认证成功: userId={}, username={}", userId, username);

            return true;
        } catch (Exception e) {
            // Access Token 无效/过期，尝试使用 Refresh Token
            log.warn("Access Token 验证失败: {}", e.getMessage());

            String refreshToken = extractToken(request, "X-Refresh-Token");
            if (refreshToken != null && jwtUtils.validateRefreshToken(refreshToken)) {
                // Refresh Token 有效，提示前端刷新 Access Token
                return unauthorized(response, "Access Token 已过期，请刷新");
            }
            // Refresh Token 也失效，返回 401
            return unauthorized(response, "Token 无效或已过期，请重新登录");
        }
    }
    /**
     * 从请求头提取 Token
     */
    private String extractToken(HttpServletRequest request, String headerName) {
        String authHeader = request.getHeader(headerName);
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }
    /**
     * 统一错误响应方法（避免重复代码）
     */
    private boolean unauthorized(HttpServletResponse response, String message) throws Exception {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(
                String.format("{\"code\":401,\"message\":\"%s\"}", message)
        );
        return false;
    }
}