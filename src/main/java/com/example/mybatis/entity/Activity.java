package com.example.mybatis.entity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;
/**
 * 活动实体类
 * @author yihui
 */
@Data
@TableName("activity")
public class Activity {
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("activity_name")
    private String activityName;

    @TableField("activity_type")
    private String activityType;

    @TableField("description")
    private String description;

    @TableField("start_time")
    private LocalDateTime startTime;

    @TableField("location")
    private String location;

    @TableField("max_participants")
    private Integer maxParticipants;

    @TableField("current_participants")
    private Integer currentParticipants;

    @TableField("contact_person")
    private String contactPerson;

    @TableField("contact_phone")
    private String contactPhone;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;

    @TableField("course_id")
    private Long courseId;
}