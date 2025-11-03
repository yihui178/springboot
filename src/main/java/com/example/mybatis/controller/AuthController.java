package com.example.mybatis.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.example.mybatis.entity.User;
import com.example.mybatis.http.HttpResult;
import com.example.mybatis.service.UserService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.*;




@RestController
public class AuthController {

    private static final String SECRET_KEY = "your-very-secret-and-long-key-12345678";
    private static final Key SIGNING_KEY = Keys.hmacShaKeyFor(SECRET_KEY.getBytes(StandardCharsets.UTF_8));
    private static final long TOKEN_EXPIRE = 36000000; // Token 有效期：10s 1小时


    public static final String ROLE_SUPER = "super";
    public static final String ROLE_ADMIN = "admin";
    public static final String ROLE_USER = "user";

    // 角色-权限映射（与前端v-perm指令对应）
    private static final Map<String, List<String>> ROLE_PERMISSIONS = new HashMap<>();

    static {
        ROLE_PERMISSIONS.put(ROLE_SUPER, Arrays.asList("user:add", "user:delete", "user:edit", "user:view", "system:config"));
        ROLE_PERMISSIONS.put(ROLE_ADMIN, Arrays.asList("user:add", "user:edit", "user:view"));
        ROLE_PERMISSIONS.put(ROLE_USER, Collections.singletonList("user:view"));
    }
    // 1. 保留你的简化用户信息接口（无需Token，临时用）
    @Autowired
    private UserService userService;

    @PostMapping("/auth/login")
    public HttpResult<Map<String, String>> login(@RequestBody Map<String, String> loginForm) {
        // 1. 接收前端参数（Vben 登录表单默认传递 username/password/captcha）
        String username = loginForm.get("username");
        String password = loginForm.get("password");
        String captchaToken = loginForm.get("captcha");

        // 2. 参数校验（前端也会校验，后端双重保障）
        if (ObjectUtils.isEmpty(username)) {
            return HttpResult.error(400, "用户名不能为空");
        }
        if (ObjectUtils.isEmpty(password)) {
            return HttpResult.error(400, "密码不能为空");
        }
        if (ObjectUtils.isEmpty(captchaToken)) {
            return HttpResult.error(400, "请先完成滑块验证");
        }
        if (captchaToken.length() < 16) {
            return HttpResult.error(401, "验证码token无效");
        }

        // 3. 数据库查询用户（按前端Mock账号逻辑，支持super/admin/user角色）
        User user = userService.lambdaQuery()
                .eq(User::getName, username)
                .in(User::getRole, Arrays.asList(ROLE_SUPER, ROLE_ADMIN, ROLE_USER)) // 仅查询有效角色
                .one();

        // 4. 身份校验（前端Mock密码为123456，后端可暂用明文，生产环境需改BCrypt）
        if (user == null) {
            return HttpResult.error(401, "账号不存在");
        }
        if (!Objects.equals(user.getPassword(), password)) {
            return HttpResult.error(401, "账号或密码错误");
        }


        // 5. 生成Token（携带用户ID和角色，供前端后续解析）
        String token = Jwts.builder()
                .setSubject(username) // 关联用户名
                .claim("userId", user.getId()) // 扩展字段：用户ID
                .claim("currentRole", user.getRole()) // 扩展字段：当前角色（适配权限控制）
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + TOKEN_EXPIRE))
                .signWith(SIGNING_KEY, SignatureAlgorithm.HS256)
                .compact();


        // 5. 返回结果（Vben 前端要求返回 accessToken）
        Map<String, String> data = new HashMap<>();
        data.put("accessToken", token);
        return HttpResult.ok(data);
    }



    @GetMapping("/user/info")
    public HttpResult<Map<String, Object>> getUserInfo(HttpServletRequest request) {
        // 1. 从请求头获取 Token（Vben 前端会自动携带 Authorization: Bearer ${token}）
        System.out.println("token: " + request.getHeader("token"));
        System.out.println("token: " + request.getHeader("Authorization"));
        String authHeader = request.getHeader("Authorization");
        if (ObjectUtils.isEmpty(authHeader) || !authHeader.startsWith("Bearer ")) {
            return HttpResult.error(401, "Token缺失，请先登录"); // 401-Token缺失
        }

        String token = authHeader.substring(7);
        // 2. 校验Token格式（避免非法Token导致500）
        if (!token.contains(".") || token.split("\\.").length != 3) {
            return HttpResult.error(401, "Token格式无效，请重新登录"); // 401-Token非法
        }
        try {

            // 2. 解析 Token，获取用户 ID
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(SIGNING_KEY)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            Long userId = claims.get("userId", Long.class);
            String currentRole = claims.get("currentRole", String.class);

            // 3. 查询用户完整信息
            User user = userService.getById(userId);
            if (user == null) {
                return HttpResult.error(401, "登录用户不存在");
            }

            // 4. 构造前端所需数据（严格包含roles和realName，扩展字段可选）
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("roles", Collections.singletonList(currentRole)); // 必须是数组（前端UserInfo接口要求）
            userInfo.put("realName", user.getName()); // 必须字段（前端显示用户名）
            // 扩展字段（前端可按需使用）
            userInfo.put("userId", user.getId());
            userInfo.put("username", user.getName());
            userInfo.put("email", user.getEmail());
            userInfo.put("perms", getPermsByRole(currentRole)); // 权限码（适配前端按钮级控制）

            System.out.println("完整信息: " + userInfo);
            return HttpResult.ok(userInfo);
            } catch (ExpiredJwtException e) {
                // Token过期：明确返回401
                return HttpResult.error(401, "Token已过期，请重新登录");
            } catch (Exception e) {
                // 其他Token错误（如签名无效）：返回401
                return HttpResult.error(401, "Token无效，请重新登录");
            }

    }

//     2. 恢复登录接口（生成Token，供前端登录调用）
//     密钥：确保≥32字节（HS256算法要求）
//
//
//
//     3. 可选：恢复刷新Token接口（避免Token过期后重新登录）
    @PostMapping("/auth/refreshToken")
    public HttpResult<Map<String, String>> refreshToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (ObjectUtils.isEmpty(authHeader) || !authHeader.startsWith("Bearer ")) {
            return HttpResult.error(401, "Token缺失，无法刷新");
        }
        String oldToken = authHeader.substring(7);

        try {
            // 解析旧Token获取用户信息
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(SIGNING_KEY)
                    .build()
                    .parseClaimsJws(oldToken)
                    .getBody();

            String username = claims.getSubject();
            Long userId = claims.get("userId", Long.class);
            String currentRole = claims.get("currentRole", String.class);

            // 生成新Token（有效期重置）
            String newToken = Jwts.builder()
                    .setSubject(username)
                    .claim("userId", userId)
                    .claim("currentRole", currentRole)
                    .setIssuedAt(new Date())
                    .setExpiration(new Date(System.currentTimeMillis() + TOKEN_EXPIRE))
                    .signWith(SIGNING_KEY, SignatureAlgorithm.HS256)
                    .compact();

            Map<String, String> data = new HashMap<>();
            data.put("accessToken", newToken);
            return HttpResult.ok(data);

        } catch (Exception e) {
            return HttpResult.error(401, "Token无效或已过期，需重新登录");
        }
    }
    @GetMapping("/auth/codes")
    public HttpResult getAccessCodes() {
        return HttpResult.ok(List.of()); // 必须返回空数组，匹配前端需求
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

    private List<String> getPermsByRole(String role) {
        return ROLE_PERMISSIONS.getOrDefault(role, Collections.emptyList());
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


}