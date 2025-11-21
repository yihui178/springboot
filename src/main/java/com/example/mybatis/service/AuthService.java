package com.example.mybatis.service;
import com.example.mybatis.dto.UserDTO;
import java.util.List;
import java.util.Map;
/**
 * 认证服务接口
 */
public interface AuthService {

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
    String refreshToken(String token);

    /**
     * 获取用户信息（通过 Token）
     */
    UserDTO getUserInfo(String token);

    /**
     * 验证滑块验证码
     */
    void verifyCaptcha(String captchaToken);

    /**
     * 获取用户的按钮权限码（通过 Token）
     */
    List<String> getAccessCodes(String token);

    /**
     * 获取用户的角色和权限（通过 Token）
     */
    Map<String, Object> getAccess(String token);

    /**
     * 根据角色ID列表获取角色代码列表
     */
    List<String> getRoleCodesByRoleIds(List<Long> roleIds);
}