package com.example.mybatis.controller;
import com.example.mybatis.common.HttpResult;
import com.example.mybatis.dto.UserDTO;
import com.example.mybatis.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
/**
 * 认证控制器（极简版）
 * 职责：仅负责接收请求和返回响应
 */
@Slf4j
@RestController
public class AuthController {
    private final AuthService authService;
    // ✅ 构造器注入（单个参数可省略 @Autowired）
    public AuthController(AuthService authService) {
        this.authService = authService;
    }
    /**
     * 用户登录
     */
    @PostMapping("/auth/login")
    public HttpResult<Map<String, Object>> login(@RequestBody Map<String, String> loginForm) {
        return HttpResult.ok(authService.login(loginForm));
    }
    /**
     * 获取用户信息
     */
    @GetMapping("/user/info")
    public HttpResult<UserDTO> getUserInfo(HttpServletRequest request) {
        String token = extractToken(request);
        return HttpResult.ok(authService.getUserInfo(token));
    }
    /**
     * 刷新 Token
     */
    @PostMapping("/auth/refreshToken")
    public HttpResult<Map<String, String>> refreshToken(HttpServletRequest request) {
        String token = extractToken(request);
        return HttpResult.ok(Map.of("accessToken", authService.refreshToken(token)));
    }
    /**
     * 登出
     */
    @PostMapping("/auth/logout")
    public HttpResult<String> logout() {
        return HttpResult.ok("登出成功");
    }
    /**
     * 用户注册
     */
    @PostMapping("/auth/register")
    public HttpResult<Void> register(@RequestBody Map<String, Object> registerForm) {
        authService.register(registerForm);
        return HttpResult.ok(null, "注册成功，请登录");
    }
    /**
     * 验证滑块验证码
     */
    @PostMapping("/auth/verifyCaptcha")
    public HttpResult<String> verifyCaptcha(@RequestBody Map<String, String> form) {
        authService.verifyCaptcha(form.get("captcha"));
        return HttpResult.ok("验证通过");
    }
    /**
     * 获取用户的按钮权限码
     */
    @GetMapping("/auth/codes")
    public HttpResult<List<String>> getAccessCodes(HttpServletRequest request) {
        String token = extractToken(request);
        return HttpResult.ok(authService.getAccessCodes(token));
    }
    /**
     * 获取用户的角色和权限码
     */
    @GetMapping("/auth/access")
    public HttpResult<Map<String, Object>> getAccess(HttpServletRequest request) {
        String token = extractToken(request);
        return HttpResult.ok(authService.getAccess(token));
    }
    // ========== 私有方法 ==========
    /**
     * 从请求头提取 Token
     */
    private String extractToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("Token缺失");
        }
        return authHeader.substring(7);
    }
}