package com.example.mybatis.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author yihui
 */
@Data
@Schema(description = "权限数据传输对象")
public class PermissionDTO {
    @Schema(description = "权限ID")
    private Long id;

    @Schema(description = "权限名称")
    private String name;

    @Schema(description = "权限代码")
    private String code;

    @Schema(description = "权限类型")
    private String type;

    @Schema(description = "路径")
    private String path;

    @Schema(description = "组件名称")
    private String component;

    @Schema(description = "父级ID")
    private Long parentId;

    @Schema(description = "排序号")
    private Integer orderNum;

    @Schema(description = "图标")
    private String icon;
}
