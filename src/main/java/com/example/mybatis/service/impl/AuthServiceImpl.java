package com.example.mybatis.service.impl;
import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.example.mybatis.common.SpringException;
import com.example.mybatis.controller.NotificationController;
import com.example.mybatis.dto.UserDTO;
import com.example.mybatis.entity.Permission;
import com.example.mybatis.entity.Role;
import com.example.mybatis.entity.User;
import com.example.mybatis.entity.UserRole;
import com.example.mybatis.service.*;
import com.example.mybatis.utils.JwtUtils;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
/**
 * 认证服务实现类
 * @author yihui
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final UserService userService;
    private final RoleService roleService;
    private final PermissionService permissionService;
    private final UserRoleService userRoleService;
    private final RolePermissionService rolePermissionService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final NotificationController notificationController;
    // ========== 公开接口实现 ==========
    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserDTO register(Map<String, Object> registerForm) {
        // 提取参数
        String username = (String) registerForm.get("username");
        String password = (String) registerForm.get("password");
        String confirmPassword = (String) registerForm.get("confirmPassword");
        Boolean agreePolicy = parseBoolean(registerForm.get("agreePolicy"));
        // 参数校验
        validateRegisterParams(username, password, confirmPassword, agreePolicy);
        // 检查用户名是否已存在
        if (userService.lambdaQuery().eq(User::getName, username).exists()) {
            throw new SpringException("用户名已被注册，请更换其他用户名", 400);
        }
        // 创建用户
        User newUser = new User();
        newUser.setName(username);
        newUser.setPassword(passwordEncoder.encode(password));
        newUser.setAge(18);
        newUser.setEmail(username + "@example.com");
        userService.save(newUser);
        // 分配默认角色
        assignDefaultRole(newUser.getId());
        // 返回 DTO
        UserDTO userDTO = new UserDTO();
        BeanUtils.copyProperties(newUser, userDTO);
        userDTO.setRole("user");
        return userDTO;
    }
    @Override
    public Map<String, Object> login(Map<String, String> loginForm) {
        // 提取参数
        String username = loginForm.get("username");
        String password = loginForm.get("password");
        String captchaToken = loginForm.get("captcha");
        // 参数校验
        validateLoginParams(username, password, captchaToken);
        verifyCaptcha(captchaToken);
        // 验证用户
        User user = userService.lambdaQuery().eq(User::getName, username).one();
        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
            throw new SpringException("账号或密码错误", 401);
        }
        // 查询角色
        List<Long> roleIds = userRoleService.listRoleIdsByUserId(user.getId());
        List<String> roleCodes = getRoleCodesByRoleIds(roleIds);
        // 生成 Token
        String accessToken = jwtUtils.generateAccessToken(user.getId(), username, roleCodes);
        String refreshToken = jwtUtils.generateRefreshToken(user.getId(), username);
        return Map.of(
                "accessToken", accessToken,
                "refreshToken", refreshToken,
                "roles", roleCodes
        );
    }
    @Override
    public String refreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new SpringException("Refresh Token 不能为空", 400);
        }
        // 验证 Refresh Token
        if (!jwtUtils.validateRefreshToken(refreshToken)) {
            throw new SpringException("Refresh Token 无效或已过期", 401);
        }
        // 解析 Token 获取 userId
        Claims claims = jwtUtils.parseClaims(refreshToken);
        Long userId = claims.get("userId", Long.class);
        User user = userService.getById(userId);
        if (user == null) {
            throw new SpringException("用户不存在", 401);
        }
        // 重新查询角色
        List<Long> roleIds = userRoleService.listRoleIdsByUserId(userId);
        List<String> roleCodes = getRoleCodesByRoleIds(roleIds);
        // 生成新的 Access Token
        return jwtUtils.generateAccessToken(userId, user.getName(), roleCodes);
    }
    @Override
    public void verifyCaptcha(String captchaToken) {
        // 校验 token 长度
        if (captchaToken == null || captchaToken.length() < 16) {
            throw new SpringException("验证码校验失败", 401);
        }

        // 提取时间戳并验证是否过期（5分钟）
        try {
            String timestamp = captchaToken.substring(captchaToken.length() - 13);
            long tokenTime = Long.parseLong(timestamp);
            if (System.currentTimeMillis() - tokenTime > 300000) {
                throw new SpringException("验证码已过期", 401);
            }
        } catch (Exception e) {
            throw new SpringException("验证码格式无效", 401);
        }
    }
    // ========== 需要登录的接口（接收 userId） ==========
    @Override
    public UserDTO getUserInfo(Long userId) {
        // 校验参数
        if (userId == null) {
            throw new SpringException("用户未登录", 401);
        }
        // 查询用户
        User user = userService.getById(userId);
        if (user == null) {
            throw new SpringException("用户不存在", 404);
        }
        // 查询角色
        List<Long> roleIds = userRoleService.listRoleIdsByUserId(userId);
        List<String> roleCodes = getRoleCodesByRoleIds(roleIds);
        // 封装 DTO
        UserDTO dto = new UserDTO();
        BeanUtils.copyProperties(user, dto);
        dto.setRole(String.join(",", roleCodes));
        return dto;
    }
    @Override
    public List<String> getAccessCodes(Long userId) {
        if (userId == null) {
            throw new SpringException("用户未登录", 401);
        }
        // 查询角色
        List<Long> roleIds = userRoleService.listRoleIdsByUserId(userId);

        // 查询权限
        List<Long> permIds = rolePermissionService.listPermissionIdsByRoleIds(roleIds);
        // 过滤按钮权限
        return permissionService.listByIds(permIds).stream()
                .filter(p -> ("button".equalsIgnoreCase(p.getType())
                        || "menu".equalsIgnoreCase(p.getType()))
                        && p.getCode() != null
                        && !p.getCode().isEmpty())
                .map(Permission::getCode)
                .distinct()
                .toList();
    }
    @Override
    public Map<String, Object> getAccess(Long userId) {
        if (userId == null) {
            throw new SpringException("用户未登录", 401);
        }
        // 查询角色
        List<Long> roleIds = userRoleService.listRoleIdsByUserId(userId);
        List<String> roleCodes = getRoleCodesByRoleIds(roleIds);
        // 查询权限
        List<Long> permIds = rolePermissionService.listPermissionIdsByRoleIds(roleIds);
        List<String> permCodes = permissionService.listByIds(permIds).stream()
                .map(Permission::getCode)
                .filter(Objects::nonNull)
                .toList();
        return Map.of("roles", roleCodes, "accessCodes", permCodes);
    }
    /**
     * 用户登出
     */
    @Override
    public void logout(Long userId) {
        if (userId != null) {
            jwtUtils.deleteRefreshToken(userId);
            // 直接调用 notificationController
            notificationController.removeConnection(userId);
        }
    }
    // ========== 辅助方法 ==========
    @Override
    public List<String> getRoleCodesByRoleIds(List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return Collections.emptyList();
        }
        return roleService.listByIds(roleIds).stream()
                .map(Role::getCode)
                .filter(Objects::nonNull)
                .toList();
    }
    // ========== 私有方法：参数校验 ==========
    private void validateRegisterParams(String username, String password,
                                        String confirmPassword, Boolean agreePolicy) {
        if (ObjectUtils.isEmpty(username) || username.length() < 2 || username.length() > 20) {
            throw new SpringException("用户名长度需在2-20位之间", 400);
        }
        if (ObjectUtils.isEmpty(password) || password.length() < 6 || password.length() > 20) {
            throw new SpringException("密码长度需在6-20位之间", 400);
        }
        if (!password.equals(confirmPassword)) {
            throw new SpringException("两次输入的密码不一致", 400);
        }
        if (!Boolean.TRUE.equals(agreePolicy)) {
            throw new SpringException("请同意用户协议后再注册", 400);
        }
    }
    private void validateLoginParams(String username, String password, String captchaToken) {
        if (ObjectUtils.isEmpty(username)) {
            throw new SpringException("用户名不能为空", 400);
        }
        if (ObjectUtils.isEmpty(password)) {
            throw new SpringException("密码不能为空", 400);
        }
        if (ObjectUtils.isEmpty(captchaToken) || captchaToken.length() < 16) {
            throw new SpringException("请先完成滑块验证", 401);
        }
    }
    private void assignDefaultRole(Long userId) {
        Role defaultRole = roleService.lambdaQuery().eq(Role::getCode, "user").one();
        if (defaultRole == null) {
            throw new SpringException("系统未配置默认用户角色，请联系管理员", 500);
        }
        UserRole userRole = new UserRole();
        userRole.setUserId(userId);
        userRole.setRoleId(defaultRole.getId());
        userRoleService.save(userRole);
    }
    private Boolean parseBoolean(Object value) {
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof String) {
            return "true".equalsIgnoreCase((String) value);
        }
        return false;
    }
}