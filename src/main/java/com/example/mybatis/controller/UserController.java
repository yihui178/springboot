package com.example.mybatis.controller;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.mybatis.utils.CheckParamUtils;
import com.example.mybatis.dto.UserDTO;
import com.example.mybatis.entity.User;
import com.example.mybatis.common.HttpResult;
import com.example.mybatis.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
/**
 * 用户管理控制器
 * @author yihui
 */
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
@Tag(name = "用户管理", description = "用户信息的增删改查接口")
public class UserController {
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    /**
     * 分页查询用户
     */
    @GetMapping("/page")
    @Operation(summary = "分页查询用户")
    public HttpResult<Map<String, Object>> page(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword) {

        // 构建查询条件
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        if (keyword != null && !keyword.trim().isEmpty()) {
            queryWrapper.and(wrapper -> wrapper
                    .like("name", keyword)
                    .or()
                    .like("email", keyword)
            );
        }

        // 分页查询
        IPage<User> pageObj = new Page<>(page, pageSize);
        IPage<User> result = userService.page(pageObj, queryWrapper);

        // 转换为 DTO
        List<UserDTO> dtoList = result.getRecords().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        // 封装返回结果
        Map<String, Object> data = new HashMap<>();
        data.put("list", dtoList);
        data.put("total", result.getTotal());
        data.put("page", result.getCurrent());
        data.put("pageSize", result.getSize());

        return HttpResult.ok(data);
    }
    /**
     * 查询所有用户
     */
    @GetMapping
    @Operation(summary = "查询所有用户")
    public HttpResult<List<UserDTO>> getAllUsers() {
        List<UserDTO> list = userService.list()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        return HttpResult.ok(list);
    }
    /**
     * 根据 ID 查询用户
     */
    @GetMapping("/{id}")
    @Operation(summary = "根据ID查询单个用户信息")
    public HttpResult<UserDTO> getUserById(@PathVariable("id") Long id) {
        CheckParamUtils.isBiggerZero(id, "用户ID");
        User user = userService.getById(id);
        if (user == null) {
            return HttpResult.error(404, "用户不存在");
        }
        UserDTO dto = convertToDTO(user);
        return HttpResult.ok(dto);
    }
    /**
     * 新增用户
     */
    @PostMapping("/add")
    @Operation(summary = "新增用户")
    public HttpResult<String> addUser(@RequestBody User user) {
        // 参数校验
        if (user.getName() == null || user.getName().trim().isEmpty()) {
            return HttpResult.error(400, "用户名不能为空");
        }
        if (user.getPassword() == null || user.getPassword().trim().isEmpty()) {
            return HttpResult.error(400, "密码不能为空");
        }
        if (user.getAge() == null || user.getAge() < 1 || user.getAge() > 120) {
            return HttpResult.error(400, "年龄必须在1-120之间");
        }
        if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
            return HttpResult.error(400, "邮箱不能为空");
        }

        // 检查用户名是否已存在
        boolean exists = userService.lambdaQuery()
                .eq(User::getName, user.getName())
                .exists();
        if (exists) {
            return HttpResult.error(400, "用户名已存在");
        }

        // 密码加密
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        // 设置默认角色（如果未指定）
        if (user.getRole() == null || user.getRole().trim().isEmpty()) {
            user.setRole("user");
        }

        // 保存用户
        boolean success = userService.save(user);
        if (success) {
            return HttpResult.ok("新增成功");
        } else {
            return HttpResult.error(500, "新增失败");
        }
    }
    /**
     * 更新用户
     */
    @PutMapping("/update")
    @Operation(summary = "更新用户")
    public HttpResult<String> updateUser(@RequestBody User user) {
        if (user.getId() == null) {
            return HttpResult.error(400, "用户ID不能为空");
        }

        User existingUser = userService.getById(user.getId());
        if (existingUser == null) {
            return HttpResult.error(404, "用户不存在");
        }

        // 参数校验
        if (user.getName() != null && user.getName().trim().isEmpty()) {
            return HttpResult.error(400, "用户名不能为空");
        }
        if (user.getAge() != null && (user.getAge() < 1 || user.getAge() > 120)) {
            return HttpResult.error(400, "年龄必须在1-120之间");
        }

        // 检查用户名是否被其他用户占用
        if (user.getName() != null && !user.getName().equals(existingUser.getName())) {
            boolean exists = userService.lambdaQuery()
                    .eq(User::getName, user.getName())
                    .ne(User::getId, user.getId())
                    .exists();
            if (exists) {
                return HttpResult.error(400, "用户名已被占用");
            }
        }

        // 如果密码不为空，则加密
        if (user.getPassword() != null && !user.getPassword().trim().isEmpty()) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        } else {
            // 密码为空，保持原密码
            user.setPassword(existingUser.getPassword());
        }

        boolean success = userService.updateById(user);
        if (success) {
            return HttpResult.ok("更新成功");
        } else {
            return HttpResult.error(500, "更新失败");
        }
    }
    /**
     * 删除用户
     */
    @PostMapping("/delete")
    @Operation(summary = "删除用户")
    public HttpResult<String> deleteUser(@RequestBody Map<String, Long> body) {
        Long id = body.get("id");
        if (id == null) {
            return HttpResult.error(400, "用户ID不能为空");
        }

        User existingUser = userService.getById(id);
        if (existingUser == null) {
            return HttpResult.error(404, "用户不存在");
        }

        // 禁止删除超级管理员
        if ("super".equals(existingUser.getRole())) {
            return HttpResult.error(403, "禁止删除超级管理员");
        }

        boolean success = userService.removeById(id);
        if (success) {
            return HttpResult.ok("删除成功");
        } else {
            return HttpResult.error(500, "删除失败");
        }
    }
    // ==================== 私有辅助方法 ====================

    /**
     * 将 User 实体转换为 UserDTO
     */
    private UserDTO convertToDTO(User user) {
        UserDTO dto = new UserDTO();
        BeanUtils.copyProperties(user, dto);
        return dto;
    }
}