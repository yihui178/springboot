package com.example.mybatis.config;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
/**
 * Web MVC 配置：注册拦截器
 * @author yihui
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {
    private final JwtInterceptor jwtInterceptor;
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/**") // 拦截所有请求
                .excludePathPatterns(
                        "/auth/login",           // 登录
                        "/auth/register",        // 注册
                        "/auth/verifyCaptcha",   // 验证码校验
                        "/auth/refreshToken",    // 刷新token
//                        "/auth/logout",
                        "/captcha/**",           // 验证码
                        "/druid/**",             // Druid监控
                        "/swagger-ui/**",        // Swagger
                        "/v3/api-docs/**",
                        "/test/**",           //测试接口（生产环境需删除）
                        "/migration/**"
                );
    }
}