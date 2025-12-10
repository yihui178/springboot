package com.example.mybatis.controller;
import com.example.mybatis.common.HttpResult;
import com.example.mybatis.dto.UserDTO;
import com.example.mybatis.service.AuthService;
import com.example.mybatis.utils.RequestUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
/**
 * 认证控制器
 * @author yihui
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    // ========== 公开接口 ==========
    /** 用户登录 */
    @PostMapping("/auth/login")
    public HttpResult<Map<String, Object>> login(@RequestBody Map<String, String> loginForm) {
        return HttpResult.ok(authService.login(loginForm));
    }
    /** 用户注册 */
    @PostMapping("/auth/register")
    public HttpResult<UserDTO> register(@RequestBody Map<String, Object> registerForm) {
        return HttpResult.ok(authService.register(registerForm), "注册成功，请登录");
    }
    /** 验证滑块验证码 */
    @PostMapping("/auth/verifyCaptcha")
    public HttpResult<String> verifyCaptcha(@RequestBody Map<String, String> form) {
        authService.verifyCaptcha(form.get("captcha"));
        return HttpResult.ok("验证通过");
    }
    /** 刷新 Token */
    @PostMapping("/auth/refreshToken")
    public HttpResult<Map<String, String>> refreshToken(@RequestBody Map<String, String> body) {
        String newAccessToken = authService.refreshToken(body.get("refreshToken"));
        return HttpResult.ok(Map.of("accessToken", newAccessToken));
    }
    // ========== 需要登录的接口 ==========
    /** 获取用户信息 */
    @GetMapping("/user/info")
    public HttpResult<UserDTO> getUserInfo(HttpServletRequest request) {
        Long userId = RequestUtils.getCurrentUserId(request);
        return HttpResult.ok(authService.getUserInfo(userId));
    }
    /** 获取用户的按钮权限码 */
    @GetMapping("/auth/codes")
    public HttpResult<List<String>> getAccessCodes(HttpServletRequest request) {
        Long userId = RequestUtils.getCurrentUserId(request);
        return HttpResult.ok(authService.getAccessCodes(userId));
    }
    /** 获取用户的角色和权限 */
    @GetMapping("/auth/access")
    public HttpResult<Map<String, Object>> getAccess(HttpServletRequest request) {
        Long userId = RequestUtils.getCurrentUserId(request);
        return HttpResult.ok(authService.getAccess(userId));
    }
    /** 登出 */
    @PostMapping("/auth/logout")
    public HttpResult<String> logout(HttpServletRequest request) {
        Long userId = RequestUtils.getCurrentUserIdSafely(request);
        if (userId != null) {
            authService.logout(userId);
        }
        return HttpResult.ok("登出成功");
    }
}