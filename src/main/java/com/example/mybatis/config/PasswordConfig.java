package com.example.mybatis.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * @author yihui
 */
@Configuration
public class PasswordConfig {

    /**
     * 全局的密码编码器，bean 名称 passwordEncoder
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        // 强度 10 是默认安全值；可根据需要调整
        return new BCryptPasswordEncoder(10);
    }
}
