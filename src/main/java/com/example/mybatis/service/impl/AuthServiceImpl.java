package com.example.mybatis.service.impl;
import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.example.mybatis.common.SpringException;
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
    // 使用 final 确保不可变性
    private final UserService userService;
    private final RoleService roleService;
    private final PermissionService permissionService;
    private final UserRoleService userRoleService;
    private final RolePermissionService rolePermissionService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    // ========== 公共接口实现 ==========
    /**
     * 用户注册
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserDTO register(Map<String, Object> registerForm) {
        // 1. 提取参数
        String username = (String) registerForm.get("username");
        String password = (String) registerForm.get("password");
        String confirmPassword = (String) registerForm.get("confirmPassword");
        Boolean agreePolicy = parseBoolean(registerForm.get("agreePolicy"));
        // 2. 参数校验
        validateRegisterParams(username, password, confirmPassword, agreePolicy);
        // 3. 检查用户名是否已存在
        if (userService.lambdaQuery().eq(User::getName, username).exists()) {
            throw new SpringException("用户名已被注册，请更换其他用户名", 400);
        }
        // 4. 创建用户
        User newUser = new User();
        newUser.setName(username);
        newUser.setPassword(passwordEncoder.encode(password));
        newUser.setAge(18);
        newUser.setEmail(username + "@example.com");
        if (!userService.save(newUser)) {
            throw new SpringException("用户保存失败", 500);
        }
        // 5. 分配默认角色
        assignDefaultRole(newUser.getId());
        // 6. 返回用户信息
        UserDTO userDTO = new UserDTO();
        BeanUtils.copyProperties(newUser, userDTO);
        userDTO.setRole("user");
        return userDTO;
    }
    /**
     * 用户登录
     */
    @Override
    public Map<String, Object> login(Map<String, String> loginForm) {
        // 1. 提取参数
        String username = loginForm.get("username");
        String password = loginForm.get("password");
        String captchaToken = loginForm.get("captcha");
        // 2. 参数校验
        validateLoginParams(username, password, captchaToken);
        // 3. 验证验证码
        verifyCaptcha(captchaToken);
        // 4. 查询用户
        User user = userService.lambdaQuery()
                .eq(User::getName, username)
                .one();
        if (user == null) {
            throw new SpringException("账号不存在", 401);
        }
        // 5. 验证密码
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new SpringException("账号或密码错误", 401);
        }
        // 6. 查询用户角色
        List<Long> roleIds = userRoleService.listRoleIdsByUserId(user.getId());
        List<String> roleCodes = getRoleCodesByRoleIds(roleIds);
        // 7. 生成 Token
        String token = jwtUtils.generateToken(user.getId(), username, roleCodes);
        // 8. 返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("accessToken", token);
        result.put("roles", roleCodes);
        return result;
    }
    /**
     * 刷新 Token
     */
    @Override
    public String refreshToken(String token) {
        try {
            Claims claims = jwtUtils.parseClaims(token);
            Long userId = claims.get("userId", Long.class);
            User user = userService.getById(userId);
            if (user == null) {
                throw new SpringException("用户不存在", 401);
            }
            // 重新查询角色
            List<Long> roleIds = userRoleService.listRoleIdsByUserId(userId);
            List<String> roleCodes = getRoleCodesByRoleIds(roleIds);
            return jwtUtils.generateToken(userId, user.getName(), roleCodes);
        } catch (Exception e) {
            log.error("Token刷新失败", e);
            throw new SpringException("Token无效或已过期，需重新登录", 401);
        }
    }
    /**
     * 获取用户信息（通过 Token）
     */
    @Override
    public UserDTO getUserInfo(String token) {
        // 1. 验证 Token 格式
        if (!token.contains(".") || token.split("\\.").length != 3) {
            throw new SpringException("Token格式无效，请重新登录", 401);
        }
        try {
            // 2. 解析 Token
            Long userId = jwtUtils.parseUserId(token);
            // 3. 查询用户
            User user = userService.getById(userId);
            if (user == null) {
                throw new SpringException("登录用户不存在", 401);
            }
            // 4. 查询角色
            List<Long> roleIds = userRoleService.listRoleIdsByUserId(userId);
            List<String> roleCodes = getRoleCodesByRoleIds(roleIds);
            // 5. 封装DTO
            UserDTO dto = new UserDTO();
            BeanUtils.copyProperties(user, dto);
            dto.setRole(String.join(",", roleCodes));
            return dto;
        } catch (Exception e) {
            log.error("Token解析失败", e);
            throw new SpringException("Token无效，请重新登录", 401);
        }
    }
    /**
     * 验证滑块验证码
     */
    @Override
    public void verifyCaptcha(String captchaToken) {
        // 1. 校验 token 长度
        if (captchaToken == null || captchaToken.length() < 16) {
            throw new SpringException("验证码校验失败", 401);
        }
        // 2. 验证 token 是否在有效期内（5分钟）
        try {
            String timestamp = captchaToken.substring(captchaToken.length() - 13);
            long tokenTime = Long.parseLong(timestamp);
            long currentTime = System.currentTimeMillis();
            // 5分钟 = 300000ms
            if (currentTime - tokenTime > 300000) {
                throw new SpringException("验证码已过期", 401);
            }
        } catch (NumberFormatException | StringIndexOutOfBoundsException e) {
            throw new SpringException("验证码格式无效", 401);
        }
    }
    /**
     * 获取用户的按钮权限码（通过 Token）
     */
    @Override
    public List<String> getAccessCodes(String token) {
        try {
            // 1. 解析 Token 获取 userId
            Long userId = jwtUtils.parseUserId(token);
            // 2. 查询用户角色
            List<Long> roleIds = userRoleService.listRoleIdsByUserId(userId);
            // 3. 查询角色权限
            List<Long> permIds = rolePermissionService.listPermissionIdsByRoleIds(roleIds);
            // 4. 过滤按钮权限
            return permissionService.listByIds(permIds)
                    .stream()
                    .filter(p -> "button".equalsIgnoreCase(p.getType()))
                    .map(Permission::getCode)
                    .filter(Objects::nonNull)
                    .toList();
        } catch (Exception e) {
            log.error("获取权限码失败", e);
            throw new SpringException("Token解析失败", 401);
        }
    }
    /**
     * 获取用户的角色和权限（通过 Token）
     */
    @Override
    public Map<String, Object> getAccess(String token) {
        try {
            // 1. 解析 Token 获取 userId
            Long userId = jwtUtils.parseUserId(token);
            // 2. 查询角色
            List<Long> roleIds = userRoleService.listRoleIdsByUserId(userId);
            List<String> roleCodes = getRoleCodesByRoleIds(roleIds);
            // 3. 查询权限码
            List<String> perms = getPermissionsByUserId(userId);
            // 4. 封装结果
            Map<String, Object> result = new HashMap<>();
            result.put("roles", roleCodes);
            result.put("accessCodes", perms);
            return result;
        } catch (Exception e) {
            log.error("获取访问权限失败", e);
            throw new SpringException("Token解析失败", 401);
        }
    }
    /**
     * 根据角色ID列表获取角色代码列表
     */
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
    // ========== 私有方法：业务逻辑封装 ==========
    /**
     * 注册参数校验
     */
    private void validateRegisterParams(String username, String password,
                                        String confirmPassword, Boolean agreePolicy) {
        if (ObjectUtils.isEmpty(username)) {
            throw new SpringException("用户名不能为空", 400);
        }
        if (username.length() < 2 || username.length() > 20) {
            throw new SpringException("用户名长度需在2-20位之间", 400);
        }
        if (ObjectUtils.isEmpty(password)) {
            throw new SpringException("密码不能为空", 400);
        }
        if (password.length() < 6 || password.length() > 20) {
            throw new SpringException("密码长度需在6-20位之间", 400);
        }
        if (!password.equals(confirmPassword)) {
            throw new SpringException("两次输入的密码不一致", 400);
        }
        if (!Boolean.TRUE.equals(agreePolicy)) {
            throw new SpringException("请同意用户协议后再注册", 400);
        }
    }
    /**
     * 登录参数校验
     */
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
    /**
     * 分配默认角色
     */
    private void assignDefaultRole(Long userId) {
        Role defaultRole = roleService.lambdaQuery()
                .eq(Role::getCode, "user")
                .one();
        if (defaultRole == null) {
            throw new SpringException("系统未配置默认用户角色，请联系管理员", 500);
        }
        UserRole userRole = new UserRole();
        userRole.setUserId(userId);
        userRole.setRoleId(defaultRole.getId());
        if (!userRoleService.save(userRole)) {
            throw new SpringException("角色分配失败", 500);
        }
    }
    /**
     * 从数据库获取指定用户的权限码列表
     */
    private List<String> getPermissionsByUserId(Long userId) {
        // 1. 查询用户拥有的角色
        List<Long> roleIds = userRoleService.listRoleIdsByUserId(userId);
        if (roleIds.isEmpty()) {
            return Collections.emptyList();
        }
        // 2. 查询角色对应的权限
        List<Long> permIds = rolePermissionService.listPermissionIdsByRoleIds(roleIds);
        if (permIds.isEmpty()) {
            return Collections.emptyList();
        }
        // 3. 查询权限code
        return permissionService.listByIds(permIds)
                .stream()
                .map(Permission::getCode)
                .filter(Objects::nonNull)
                .toList();
    }
    /**
     * 解析 Boolean 类型（兼容 String/Boolean）
     */
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