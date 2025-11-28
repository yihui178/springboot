package com.example.mybatis.controller;
import com.example.mybatis.utils.CheckParamUtils;
import com.example.mybatis.dto.UserDTO;
import com.example.mybatis.entity.User;
import com.example.mybatis.common.HttpResult;
import com.example.mybatis.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.stream.Collectors;
/**
 * 用户管理控制器
 *
 * @author yh
 * @since 2025-09-29
 */
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
@Tag(name = "用户管理", description = "用户信息的查询接口")
public class UserController {

    private final UserService userService;
    /**
     * 查询所有用户
     */
    @GetMapping
    @Operation(summary = "查询所有用户", description = "结果按创建时间升序排列")
    public HttpResult<List<UserDTO>> getAllUsers() {
        // 使用泛型，简化 stream 操作
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
        // 参数校验
        CheckParamUtils.isBiggerZero(id, "用户ID");
        // 查询用户
        User user = userService.getById(id);
        if (user == null) {
            return HttpResult.error(404, "用户不存在");
        }
        // 转换为 DTO
        UserDTO dto = convertToDTO(user);
        return HttpResult.ok(dto);
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