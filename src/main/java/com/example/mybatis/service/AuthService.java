package com.example.mybatis.service;
import com.example.mybatis.dto.UserDTO;
import java.util.List;
import java.util.Map;
/**
 * 认证服务接口 - 保留原方法名版本
 * @author yihui
 */
public interface AuthService {
    // ========== 公开接口（无需登录） ==========

    /**
     * 用户注册
     */
    UserDTO register(Map<String, Object> registerForm);

    /**
     * 用户登录
     */
    Map<String, Object> login(Map<String, String> loginForm);

    /**
     * 刷新 Token
     */
    String refreshToken(String refreshToken);

    /**
     * 验证滑块验证码
     */
    void verifyCaptcha(String captchaToken);
    // ========== 需要登录的接口（接收 userId 参数） ==========

    /**
     * 获取用户信息（通过 userId）
     */
    UserDTO getUserInfo(Long userId);

    /**
     * 获取用户的按钮权限码（通过 userId）
     */
    List<String> getAccessCodes(Long userId);

    /**
     * 获取用户的角色和权限（通过 userId）
     */
    Map<String, Object> getAccess(Long userId);

    /**
     * 登出（通过 userId）
     */
    void logout(Long userId);
    // ========== 辅助方法 ==========

    /**
     * 根据角色ID列表获取角色代码列表
     */
    List<String> getRoleCodesByRoleIds(List<Long> roleIds);
}