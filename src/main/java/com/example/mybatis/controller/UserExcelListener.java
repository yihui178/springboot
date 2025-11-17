package com.example.mybatis.controller;

import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;
import com.example.mybatis.entity.User;
import com.example.mybatis.service.UserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class UserExcelListener implements ReadListener<User> {

    private static final int BATCH_COUNT = 5000;
    private final List<User> cachedDataList = new ArrayList<>(BATCH_COUNT);

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    // ➕ 去重缓存（当前 Excel 导入过程）
    private final Set<String> excelNameSet = new HashSet<>();

    public UserExcelListener(UserService userService, PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void invoke(User data, AnalysisContext context) {
        if (data.getName() == null) {
            return; // 跳过无效数据
        }

        String username = data.getName().trim();

        // ① Excel 内部重复过滤
        if (!excelNameSet.add(username)) {
            return;
        }

        // ② 密码加密（如果不是BCrypt）
        String rawPwd = data.getPassword();
        if (rawPwd != null && !(rawPwd.startsWith("$2a$") || rawPwd.startsWith("$2b$") || rawPwd.startsWith("$2y$"))) {
            data.setPassword(passwordEncoder.encode(rawPwd));
        }

        // ③ 交给 DB 自动生成主键
        data.setId(null);

        cachedDataList.add(data);

        if (cachedDataList.size() >= BATCH_COUNT) {
            saveData();
        }
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext context) {
        saveData();
    }

    private void saveData() {
        if (cachedDataList.isEmpty()) {
            return;
        }

        // ④ 一次查数据库：查已有用户名
        List<String> names = cachedDataList.stream().map(User::getName).toList();

        Set<String> exists = userService.lambdaQuery()
                .in(User::getName, names)
                .list()
                .stream()
                .map(User::getName)
                .collect(Collectors.toSet());

        // ⑤ 过滤掉数据库已经存在的
        List<User> toSave = cachedDataList.stream()
                .filter(u -> !exists.contains(u.getName()))
                .toList();

        if (!toSave.isEmpty()) {
            userService.saveBatch(toSave, BATCH_COUNT);
        }

        cachedDataList.clear();
    }
}
