package com.example.mybatis.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author yihui
 */
@Data
@Schema(description = "角色数据传输对象")
public class RoleDTO {
    @Schema(description = "角色ID")
    private Long id;

    @Schema(description = "角色名称")
    private String name;

    @Schema(description = "角色代码")
    private String code;

    @Schema(description = "描述")
    private String description;
}
