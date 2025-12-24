package com.example.mybatis.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 活动数据传输对象
 * @author yihui
 */
@Data
@Schema(description = "活动数据传输对象")
public class ActivityDTO {
    @Schema(description = "活动ID")
    private Long id;

    @NotBlank(message = "活动名称不能为空")
    @Schema(description = "活动名称")
    private String activityName;

    @NotBlank(message = "活动类型不能为空")
    @Schema(description = "活动类型")
    private String activityType;

    @NotBlank(message = "活动描述不能为空")
    @Schema(description = "活动描述")
    private String description;

    @NotNull(message = "开始时间不能为空")
    @Schema(description = "开始时间")
    private LocalDateTime startTime;

    @NotBlank(message = "活动地点不能为空")
    @Schema(description = "活动地点")
    private String location;

    @Schema(description = "最大参与人数")
    private Integer maxParticipants;

    @Schema(description = "当前报名人数")
    private Integer currentParticipants;

    @Schema(description = "联系人")
    private String contactPerson;

    @Schema(description = "联系电话")
    private String contactPhone;
}