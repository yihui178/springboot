package com.example.mybatis;

import com.example.mybatis.entity.User;
import com.example.mybatis.mapper.UserMapper;
import com.example.mybatis.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class MybatisApplicationTests {
    @Autowired
    protected UserService userService;

    // 测试查询所有用户
    @Test
    void contextLoads() {
        List<User> users = userService.list();
        assertNotNull(users, "查询结果为空"); // 断言结果不为null
        // 若之前新增过数据，可进一步断言数量：assertTrue(users.size() > 0);
    }

}
