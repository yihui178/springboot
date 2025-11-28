package com.example.mybatis.controller;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;
import com.example.mybatis.entity.Role;
import com.example.mybatis.entity.User;
import com.example.mybatis.entity.UserRole;
import com.example.mybatis.service.RoleService;
import com.example.mybatis.service.UserRoleService;
import com.example.mybatis.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
/**
 * @author yihui
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserExcelListener implements ReadListener<User> {
    private static final int BATCH_COUNT = 5000;
    private final List<User> cachedDataList = new ArrayList<>(BATCH_COUNT);
    private final Set<String> excelNameSet = new HashSet<>();

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final UserRoleService userRoleService;
    private final RoleService roleService;

    @Override
    public void invoke(User data, AnalysisContext context) {
        if (data.getName() == null) {return;}

        String username = data.getName().trim();
        if (!excelNameSet.add(username)) {return;}

        // 密码加密
        String rawPwd = data.getPassword();
        if (rawPwd != null && !rawPwd.startsWith("$2a$")) {
            data.setPassword(passwordEncoder.encode(rawPwd));
        }

        cachedDataList.add(data);

        if (cachedDataList.size() >= BATCH_COUNT) {
            saveData();
        }
    }
    @Override
    public void doAfterAllAnalysed(AnalysisContext context) {
        saveData();
        log.info("Excel 数据导入完成");
    }
    private void saveData() {
        if (cachedDataList.isEmpty()) {return;}
        // 查询已存在的用户
        List<String> names = cachedDataList.stream().map(User::getName).toList();
        Set<String> exists = userService.lambdaQuery()
                .in(User::getName, names)
                .list()
                .stream()
                .map(User::getName)
                .collect(Collectors.toSet());
        // 过滤新用户
        List<User> toSave = cachedDataList.stream()
                .filter(u -> !exists.contains(u.getName()))
                .toList();
        if (!toSave.isEmpty()) {
            // 保存用户
            userService.saveBatch(toSave, BATCH_COUNT);
            log.info("保存 {} 个用户", toSave.size());
            // 分配默认角色
            assignDefaultRoleToUsers(toSave);
        }
        cachedDataList.clear();
    }
    private void assignDefaultRoleToUsers(List<User> users) {
        try {
            Role defaultRole = roleService.lambdaQuery()
                    .eq(Role::getCode, "user")
                    .one();
            if (defaultRole == null) {
                log.error("系统未配置默认角色 'user'");
                return;
            }
            List<UserRole> userRoles = new ArrayList<>();
            for (User user : users) {
                // 如果 Excel 中指定了角色，使用指定的角色
                String roleCode = user.getRole();
                if (roleCode == null || roleCode.trim().isEmpty()) {
                    // 默认角色
                    roleCode = "user";
                }
                // 查询角色
                Role role = roleService.lambdaQuery()
                        .eq(Role::getCode, roleCode)
                        .one();
                if (role == null) {
                    log.warn("角色 '{}' 不存在，使用默认角色", roleCode);
                    role = defaultRole;
                }
                // 创建用户角色关系
                UserRole ur = new UserRole();
                ur.setUserId(user.getId());
                ur.setRoleId(role.getId());
                userRoles.add(ur);
                // 更新 user.role 字段
                user.setRole(role.getCode());
            }
            // 批量插入 user_role 表
            userRoleService.saveBatch(userRoles, BATCH_COUNT);
            // 批量更新 user.role 字段
            userService.updateBatchById(users, BATCH_COUNT);
            log.info("为 {} 个用户分配了角色", users.size());
        } catch (Exception e) {
            log.error("分配角色失败: {}", e.getMessage(), e);
        }
    }
}