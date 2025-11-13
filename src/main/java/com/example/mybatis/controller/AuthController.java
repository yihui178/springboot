package com.example.mybatis.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.example.mybatis.dto.UserDTO;
import com.example.mybatis.entity.User;
import com.example.mybatis.http.HttpResult;
import com.example.mybatis.service.*;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import com.example.mybatis.entity.Permission;
import com.example.mybatis.common.*;


import org.springframework.web.bind.annotation.*;

import java.util.*;




@RestController
public class AuthController {


    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private UserService userService;

    @Autowired
    private RoleService roleService;

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private UserRoleService userRoleService;

    @Autowired
    private RolePermissionService rolePermissionService;

    @PostMapping("/auth/login")
    public HttpResult<Map<String, Object>> login(@RequestBody Map<String, String> loginForm) {
        String username = loginForm.get("username");
        String password = loginForm.get("password");
        String captchaToken = loginForm.get("captcha");

        if (ObjectUtils.isEmpty(username)) {
            return HttpResult.error(400, "用户名不能为空");
        }
        if (ObjectUtils.isEmpty(password)) {
            return HttpResult.error(400, "密码不能为空");
        }
        if (ObjectUtils.isEmpty(captchaToken) || captchaToken.length() < 16) {
            return HttpResult.error(401, "验证码token无效");
        }

        // 1. 只按用户名查，不再限制固定角色
        User user = userService.lambdaQuery()
                .eq(User::getName, username)
                .one();

        if (user == null) {
            return HttpResult.error(401, "账号不存在");
        }
        if (!Objects.equals(user.getPassword(), password)) {
            return HttpResult.error(401, "账号或密码错误");
        }

        // 2. 查这个用户拥有哪些角色（来自 user_role 表）
        List<Long> roleIds = userRoleService.listRoleIdsByUserId(user.getId());
        // 根据 roleIds 查角色编码，比如 admin、user...
        List<String> roleCodes = roleService.listByIds(roleIds).stream()
                .map(role -> role.getCode())   // 看你角色表字段叫什么，可能是getRoleCode()/getName()
                .filter(Objects::nonNull)
                .toList();

        // 3. 查权限
        List<String> perms = getPermissionsByUserId(user.getId());

        // 4. 生成 token —— 可以把角色也放进去，前端有时候喜欢直接从token里拿
        String token = jwtUtils.generateToken(user.getId(), username, roleCodes);

        Map<String, Object> data = new HashMap<>();
        data.put("accessToken", token);
        data.put("roles", roleCodes);
//        data.put("permissions", perms);
        return HttpResult.ok(data);
    }




    @GetMapping("/user/info")
    public HttpResult<UserDTO> getUserInfo(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (ObjectUtils.isEmpty(authHeader) || !authHeader.startsWith("Bearer ")) {
            return HttpResult.error(401, "Token缺失，请先登录");
        }

        String token = authHeader.substring(7);
        if (!token.contains(".") || token.split("\\.").length != 3) {
            return HttpResult.error(401, "Token格式无效，请重新登录");
        }

        try {
            Claims claims = jwtUtils.parseClaims(token);

            Long userId = claims.get("userId", Long.class);

            User user = userService.getById(userId);
            if (user == null) {
                return HttpResult.error(401, "登录用户不存在");
            }

            // 1. 查角色
            List<Long> roleIds = userRoleService.listRoleIdsByUserId(userId);
            List<String> roleCodes = roleService.listByIds(roleIds).stream()
                    .map(role -> role.getCode())
                    .filter(Objects::nonNull)
                    .toList();

            // 2. 查权限
            UserDTO dto = new UserDTO();
            dto.setId(user.getId());
            dto.setName(user.getName());
            dto.setEmail(user.getEmail());
            dto.setRole(String.join(",", roleCodes));
            return HttpResult.ok(dto);
        } catch (ExpiredJwtException e) {
            return HttpResult.error(401, "Token已过期，请重新登录");
        } catch (Exception e) {
            return HttpResult.error(401, "Token无效，请重新登录");
        }
    }



//     3. 可选：恢复刷新Token接口（避免Token过期后重新登录）
    @PostMapping("/auth/refreshToken")
    public HttpResult<Map<String, String>> refreshToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (ObjectUtils.isEmpty(authHeader) || !authHeader.startsWith("Bearer ")) {
            return HttpResult.error(401, "Token缺失，无法刷新");
        }
        String oldToken = authHeader.substring(7);

        try {
            Claims claims = jwtUtils.parseClaims(oldToken);

            Long userId = claims.get("userId", Long.class);
            User user = userService.getById(userId);
            if (user == null) {
                return HttpResult.error(401, "用户不存在");
            }

// 重新查角色
            List<Long> roleIds = userRoleService.listRoleIdsByUserId(userId);
            List<String> roleCodes = roleService.listByIds(roleIds).stream()
                    .map(role -> role.getCode())
                    .filter(Objects::nonNull)
                    .toList();

            String newToken = jwtUtils.generateToken(userId, user.getName(), roleCodes);

            Map<String, String> data = new HashMap<>();
            data.put("accessToken", newToken);
            return HttpResult.ok(data);



        } catch (Exception e) {
            return HttpResult.error(401, "Token无效或已过期，需重新登录");
        }
    }



    @GetMapping("/auth/codes")
    public HttpResult<List<String>> getAccessCodes(HttpServletRequest request) {
        try {
            // 1️⃣ 从 Token 获取 userId
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return HttpResult.error(401, "缺少Token");
            }
            String token = authHeader.substring(7);
            Long userId = jwtUtils.parseUserId(token);

            // 2️⃣ 获取所有权限码
            List<Long> roleIds = userRoleService.listRoleIdsByUserId(userId);
            List<Long> permIds = rolePermissionService.listPermissionIdsByRoleIds(roleIds);

            // 3️⃣ 查询仅按钮权限
            List<String> codes = permissionService.listByIds(permIds)
                    .stream()
                    .filter(p -> "button".equalsIgnoreCase(p.getType()))
                    .map(Permission::getCode)
                    .filter(Objects::nonNull)
                    .toList();

            return HttpResult.ok(codes);
        } catch (Exception e) {
            e.printStackTrace();
            return HttpResult.error(401, "Token解析失败");
        }
    }



    // 2. 补充/auth/logout接口（前端登出时调用，清除Token即可）
    @PostMapping("/auth/logout")
    public HttpResult<String> logout() {
        // 若生产环境用Redis存储Token，需在此处删除当前用户的Token
        // 目前简化处理，直接返回成功
        return HttpResult.ok("登出成功");
    }

    @PostMapping("/auth/register")
    public HttpResult<Void> register(@RequestBody Map<String, Object> registerForm) {
        try {
            // 1. 接收并处理前端参数（重点：确保agreePolicy类型正确）
            String username = (String) registerForm.get("username");
            String password = (String) registerForm.get("password");
            String confirmPassword = (String) registerForm.get("confirmPassword");

            // 处理agreePolicy：前端是boolean，后端需兼容可能的类型转换（如String转boolean）
            Boolean agreePolicy = false;
            Object agreePolicyObj = registerForm.get("agreePolicy");
            if (agreePolicyObj instanceof Boolean) {
                agreePolicy = (Boolean) agreePolicyObj;
            } else if (agreePolicyObj instanceof String) {
                // 若前端意外传递字符串（如"true"），转为boolean
                agreePolicy = "true".equalsIgnoreCase((String) agreePolicyObj);
            }

            // 2. 基础参数校验（与前端逻辑对齐）
            if (ObjectUtils.isEmpty(username)) {
                return HttpResult.error(400, "用户名不能为空");
            }
            if (username.length() < 2 || username.length() > 20) {
                return HttpResult.error(400, "用户名长度需在2-20位之间");
            }
            if (ObjectUtils.isEmpty(password)) {
                return HttpResult.error(400, "密码不能为空");
            }
            if (password.length() < 6 || password.length() > 20) {
                return HttpResult.error(400, "密码长度需在6-20位之间");
            }
            if (ObjectUtils.isEmpty(confirmPassword) || !password.equals(confirmPassword)) {
                return HttpResult.error(400, "两次输入的密码不一致");
            }
            if (!agreePolicy) { // 必须同意协议
                return HttpResult.error(400, "请同意用户协议后再注册");
            }

            // 3. 用户名唯一性校验（避免重复注册）
            LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(User::getName, username);
            User existingUser = userService.getOne(queryWrapper);
            if (existingUser != null) {
                return HttpResult.error(400, "用户名已被注册，请更换其他用户名");
            }

            // 4. 构造用户对象（适配现有数据库字段）
            User newUser = new User();
            newUser.setName(username); // 前端username对应数据库name字段
            newUser.setPassword(password); // 明文存储（按需求）
            newUser.setAge(18); // 默认年龄（前端未传）
            newUser.setEmail(username + "@example.com"); // 默认邮箱
            newUser.setRole("user"); // 注册用户默认user角色

            // 5. 保存数据库（MyBatis-Plus）
            boolean saveSuccess = userService.save(newUser);
            if (!saveSuccess) {
                return HttpResult.error(500, "注册失败，请稍后重试");
            }

            // 6. 注册成功：返回自定义消息（前端会显示该提示并跳转登录页）
            return HttpResult.ok(null, "注册成功，请登录");

        } catch (Exception e) {
            // 捕获所有异常，返回明确错误信息（避免前端无提示）
            e.printStackTrace(); // 后端日志打印异常详情
            return HttpResult.error(500, "注册失败：" + e.getMessage().substring(0, 50)); // 截断过长消息
        }
    }



    @PostMapping("/auth/verifyCaptcha")
    public HttpResult<String> verifyCaptcha(@RequestBody Map<String, String> captchaForm) {
        // 1. 接收前端传递的token（即滑块验证通过后生成的随机字符串）
        String captchaToken = captchaForm.get("captcha");
        if (ObjectUtils.isEmpty(captchaToken)) {
            return HttpResult.error(400, "验证码token不能为空");
        }
        // 2. 校验token有效性（此处仅做演示，实际可结合Redis或数据库存储token，或简单校验格式）
        // 示例：token长度需≥16位
        if (captchaToken.length() < 16) {
            return HttpResult.error(401, "验证码token无效");
        }
        // 3. 校验通过，返回成功
        System.out.println("====================captchaToken:"+captchaToken);
        return HttpResult.ok("后端验证码校验通过");
    }

    @GetMapping("/auth/access")
    public HttpResult<Map<String, Object>> getAccess(HttpServletRequest request) {
        try {
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return HttpResult.error(401, "缺少Token");
            }
            String token = authHeader.substring(7);
            Long userId = jwtUtils.parseUserId(token);


            // 查角色
            List<Long> roleIds = userRoleService.listRoleIdsByUserId(userId);
            List<String> roleCodes = roleService.listByIds(roleIds).stream()
                    .map(r -> r.getCode())
                    .filter(Objects::nonNull)
                    .toList();

            // 查权限码
            List<String> perms = getPermissionsByUserId(userId);

            Map<String, Object> result = new HashMap<>();
            result.put("roles", roleCodes);
            result.put("accessCodes", perms);

            return HttpResult.ok(result);
        } catch (Exception e) {
            e.printStackTrace();
            return HttpResult.error(401, "Token解析失败");
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


}