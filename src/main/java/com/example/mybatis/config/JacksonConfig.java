package com.example.mybatis.config;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
/**
 * Jackson 全局配置
 * 作用：将 Long 类型的 ID 序列化为 String，避免前端 JavaScript 精度丢失
 *
 * 示例：
 * - 改造前：{"id": 1993608850932699138}  → 前端接收：1993608850932699100 (精度丢失)
 * - 改造后：{"id": "1993608850932699138"} → 前端接收："1993608850932699138" (完整保留)
 */
@Configuration
public class JacksonConfig {
    @Bean
    public ObjectMapper jacksonObjectMapper(Jackson2ObjectMapperBuilder builder) {
        // 创建 ObjectMapper
        ObjectMapper objectMapper = builder.createXmlMapper(false).build();

        // 创建自定义模块
        SimpleModule simpleModule = new SimpleModule();

        // ✅ 将 Long 和 long 类型序列化为 String
        simpleModule.addSerializer(Long.class, ToStringSerializer.instance);
        simpleModule.addSerializer(Long.TYPE, ToStringSerializer.instance);

        // 注册模块
        objectMapper.registerModule(simpleModule);

        return objectMapper;
    }
}