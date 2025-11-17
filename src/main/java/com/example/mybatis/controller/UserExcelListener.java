package com.example.mybatis.controller;

import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;
import com.example.mybatis.entity.User;
import com.example.mybatis.service.UserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class UserExcelListener implements ReadListener<User> {

    private static final int BATCH_COUNT = 5000;
    private final List<User> cachedDataList = new ArrayList<>(BATCH_COUNT);
    private final UserService userService;

    private final PasswordEncoder passwordEncoder;

    public UserExcelListener(UserService userService, PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void invoke(User data, AnalysisContext context) {

        // Excel 中的密码字段
        String rawPwd = data.getPassword();

        // 注入 PasswordEncoder（你需要新增）
        if (rawPwd != null && !rawPwd.startsWith("$2a$") && !rawPwd.startsWith("$2b$") && !rawPwd.startsWith("$2y$")) {
            // 明文 → 加密
            data.setPassword(passwordEncoder.encode(rawPwd));
        }

        // 不检查重复，交给数据库主键约束处理
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
        if (!cachedDataList.isEmpty()) {
            // 批量写入，每批 5000 条
            userService.saveBatch(cachedDataList, BATCH_COUNT);
            cachedDataList.clear();
        }
    }
}
