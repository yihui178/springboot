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
                // 拦截所有请求
                .addPathPatterns("/**")
                .excludePathPatterns(
                        // 登录
                        "/auth/login",
                        // 注册
                        "/auth/register",
                        // 验证码校验
                        "/auth/verifyCaptcha",
                        // 刷新token
                        "/auth/refreshToken",
                        // 验证码
                        "/captcha/**",
                        // Druid监控
                        "/druid/**",
                        // Swagger
                        "/swagger-ui/**",
                        "/v3/api-docs/**",

//                        "/migration/**",
                        // SSE 连接
                        "/notification/sse"
                        );
    }
}