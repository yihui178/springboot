package com.example.mybatis.generator;

import com.baomidou.mybatisplus.generator.FastAutoGenerator;
import com.baomidou.mybatisplus.generator.config.OutputFile;
import com.baomidou.mybatisplus.generator.engine.VelocityTemplateEngine;
import java.util.Collections;

public class CodeGenerator {

    public static void main(String[] args) {
        // 数据库连接配置
        String url = "jdbc:mysql://localhost:3306/userdb?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
        String username = "root";
        String password = "123456";

        // 代码生成器
        FastAutoGenerator.create(url, username, password)
                // 全局配置
                .globalConfig(builder -> {
                    builder.author("yh") // 设置作者
                            .outputDir(System.getProperty("user.dir") + "/src/main/java") // 输出目录
                            .commentDate("yyyy-MM-dd") // 注释日期
                            .disableOpenDir(); // 生成后不打开文件夹
                })
                // 包配置
                .packageConfig(builder -> {
                    builder.parent("com.example.mybatis") // 父包名
                            .moduleName("") // 子模块名，无则留空
                            .entity("entity") // 实体类包名
                            .mapper("mapper") // Mapper接口包名
                            .service("service") // Service接口包名
                            .serviceImpl("service.impl") // Service实现类包名
                            .controller("controller") // Controller包名
                            // Mapper XML文件输出路径
                            .pathInfo(Collections.singletonMap(OutputFile.xml,
                                    System.getProperty("user.dir") + "/src/main/resources/mapper"));
                })
                // 策略配置
                .strategyConfig(builder -> {
                    builder.addInclude("user", "role") // 需要生成的表名，多个表用逗号分隔
                            .addTablePrefix("t_", "sys_") // 表前缀，生成类名时会去除

                            // 实体类策略
                            .entityBuilder()
                            .enableLombok() // 开启Lombok
                            .enableTableFieldAnnotation() // 生成字段注解
                            .logicDeleteColumnName("deleted") // 逻辑删除字段

                            // Controller策略
                            .controllerBuilder()
                            .enableRestStyle() // 生成@RestController
                            .enableHyphenStyle() // URL中驼峰转连字符

                            // Service策略
                            .serviceBuilder()
                            .formatServiceFileName("%sService") // Service接口命名格式
                            .formatServiceImplFileName("%sServiceImpl"); // Service实现类命名格式
                })
                // 模板引擎配置
                .templateEngine(new VelocityTemplateEngine())
                // 执行生成
                .execute();
    }
}
