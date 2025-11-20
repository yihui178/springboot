package com.example.mybatis.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

@Data
@Schema(description = "课程数据传输对象")
public class CourseDTO {
    @Schema(description = "课程ID")
    private Long id;

    @NotBlank(message = "课程名称不能为空")
    @Size(max = 20, message = "课程名称长度不能超过20个字符")
    @Schema(description = "课程名称", example = "Java 基础入门")
    private String courseName;

    @NotBlank(message = "课程分类不能为空")
    @Schema(description = "课程分类", example = "编程语言")
    private String category;

    @NotBlank(message = "课程描述不能为空")
    @Size(max = 100, message = "课程描述长度不能超过100个字符")
    @Schema(description = "课程描述")
    private String description;

    @NotNull(message = "是否线上课程不能为空")
    @Schema(description = "是否线上课程")
    private Boolean online;

    @Schema(description = "亮点数组（表单提交）")
    private List<Long> highlightIds;
}