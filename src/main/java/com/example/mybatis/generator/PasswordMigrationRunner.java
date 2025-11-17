//package com.example.mybatis.generator;
//
//import com.example.mybatis.entity.User;
//import com.example.mybatis.service.UserService;
//import org.springframework.boot.CommandLineRunner;
//import org.springframework.stereotype.Component;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.security.crypto.password.PasswordEncoder;
//
//import java.util.List;
//数据库明文密码一次性加密
//@Component
//public class PasswordMigrationRunner implements CommandLineRunner {
//
//    @Autowired
//    private UserService userService;
//
//    @Autowired
//    private PasswordEncoder passwordEncoder;
//
//    @Override
//    public void run(String... args) throws Exception {
//        // 小心：先备份数据库！
//        List<User> users = userService.list();
//        for (User u : users) {
//            String pwd = u.getPassword();
//            if (pwd == null) continue;
//            // 简单判断：如果不是 BCrypt（以 $2a$/$2b$/$2y$ 开头），则认为是明文并进行加密
//            if (!(pwd.startsWith("$2a$") || pwd.startsWith("$2b$") || pwd.startsWith("$2y$"))) {
//                String encoded = passwordEncoder.encode(pwd);
//                u.setPassword(encoded);
//                userService.updateById(u);
//                System.out.println("Migrated userId=" + u.getId());
//            }
//        }
//        System.out.println("Password migration finished.");
//    }
//}
