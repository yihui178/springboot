package com.example.mybatis.controller;

import com.example.mybatis.common.CheckParamUtils;
import com.example.mybatis.entity.User;
import com.example.mybatis.http.HttpResult;
import com.example.mybatis.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author yh
 * @since 2025-09-29
 */

@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    protected UserService userService;

    @Tag(name = "查询用户信息接口")
//     测试查询所有用户
    @Operation(
            summary = "查询所有的用户信息",
            description = "结果按创建时间升序排列"
    )
    @GetMapping
    public HttpResult getAllUsers() {
        try {
            // 校验参数
            // CheckParamUtils.isBiggerZero(10L, "用户ID");  // 示例
            // 查询所有用户
            return HttpResult.ok(userService.list());
        } catch (IllegalArgumentException e) {
            return HttpResult.error(400, e.getMessage());
        } catch (Exception e) {
            return HttpResult.error(500, "未知错误");
        }
    }
    @Tag(name = "查询用户信息接口",description = "用户信息查询功能")
    @GetMapping("/{id}")
    @Operation(summary = "根据ID查询单个用户信息")
    public HttpResult getUserById(@PathVariable("id") Long id) {
        try {
            // 参数校验
            CheckParamUtils.isBiggerZero(id, "用户ID");

            User user = userService.getById(id);
            if (user == null) {
                return HttpResult.error(404, "用户不存在");
            }

            return HttpResult.ok(user);
        } catch (IllegalArgumentException e) {
            // 捕获参数校验异常
            return HttpResult.error(400, e.getMessage());
        } catch (Exception e) {
            // 捕获系统异常
            return HttpResult.error(500, "服务器内部错误");
        }
    }




}
