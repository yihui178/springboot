package com.example.mybatis.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * Course 数据传输对象（用于 Controller 层请求/响应）
 */
@Data
@Schema(description = "课程数据传输对象")
public class CourseDTO {
    @Schema(description = "课程ID")
    private Long id;

    @Schema(description = "课程名称", example = "Java 基础入门")
    private String courseName;

    @Schema(description = "课程分类", example = "编程语言")
    private String category;

    @Schema(description = "课程描述")
    private String description;

    @Schema(description = "是否线上课程")
    private Boolean online;

    @Schema(description = "亮点数组（表单提交）")
    private List<String> highlights;

}
