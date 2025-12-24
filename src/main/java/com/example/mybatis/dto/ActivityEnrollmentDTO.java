package com.example.mybatis.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 活动报名数据传输对象
 * @author yihui
 */
@Data
@Schema(description = "活动报名数据传输对象")
public class ActivityEnrollmentDTO {
    @Schema(description = "报名ID")
    private Long id;

    @NotNull(message = "活动ID不能为空")
    @Schema(description = "活动ID")
    private Long activityId;

    @Schema(description = "会员ID")
    private Long memberId;

    @NotBlank(message = "报名姓名不能为空")
    @Schema(description = "报名姓名")
    private String memberName;

    @NotBlank(message = "联系电话不能为空")
    @Schema(description = "联系电话")
    private String memberPhone;

    @Schema(description = "报名状态")
    private String enrollmentStatus;

    @Schema(description = "报名时间")
    private LocalDateTime enrollmentTime;

    @Schema(description = "备注")
    private String remark;

    // 扩展字段（用于列表展示）
    @Schema(description = "活动名称")
    private String activityName;
    @Schema(description = "报名类型")
    private String enrollmentType;
    @Schema(description = "课程ID")
    private Long courseId;
    @Schema(description = "课程名称")
    private String courseName;
}