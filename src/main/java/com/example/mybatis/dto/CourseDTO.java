package com.example.mybatis.dto;

import com.example.mybatis.entity.Highlight;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * 课程数据传输对象
 * @author yihui
 */
@Data
@Schema(description = "课程数据传输对象")
public class CourseDTO {
    @Schema(description = "课程ID")
    private Long id;

    @NotBlank(message = "课程名称不能为空")
    @Size(max = 20, message = "课程名称长度不能超过20个字符")
    @Schema(description = "课程名称", example = "新手骑行入门")
    private String courseName;

    @NotBlank(message = "课程分类不能为空")
    @Schema(description = "课程分类", example = "新手入门")
    private String category;

    @NotBlank(message = "课程描述不能为空")
    @Size(max = 100, message = "课程描述长度不能超过100个字符")
    @Schema(description = "课程描述")
    private String description;

    @NotNull(message = "是否线上课程不能为空")
    @Schema(description = "是否线上课程")
    private Boolean online;

    // 新增字段
    @Schema(description = "上课时间", example = "每周三 19:00-21:00")
    private String scheduleTime;

    @Schema(description = "课程时长", example = "2小时")
    private String duration;

    @Schema(description = "讲师/教练", example = "张教练")
    private String instructor;

    @Schema(description = "上课地点", example = "北京市朝阳区训练场")
    private String location;
    // 最大报名人数
    @Schema(description = "最大报名人数（0表示不限）", example = "20")
    private Integer maxStudents;
    // 新增字段：联系电话
    @Schema(description = "联系电话", example = "13800138000")
    private String contactPhone;

    @Schema(description = "亮点数组（表单提交）")
    private List<Long> highlightIds;

    // 用于返回时展示亮点详情
    @Schema(description = "亮点详情列表")
    private List<Highlight> highlights;
}