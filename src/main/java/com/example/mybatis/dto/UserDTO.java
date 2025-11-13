package com.example.mybatis.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "用户数据传输对象")
public class UserDTO {
    @Schema(description = "用户ID")
    private Long id;

    @Schema(description = "用户名")
    private String name;

    @Schema(description = "年龄")
    private Integer age;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "角色")
    private String role;
}
