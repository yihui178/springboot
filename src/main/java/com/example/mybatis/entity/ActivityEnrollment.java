package com.example.mybatis.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 活动报名实体类
 * @author yihui
 */
@Data
@TableName("activity_enrollment")
public class ActivityEnrollment {
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("activity_id")
    private Long activityId;

    @TableField("member_id")
    private Long memberId;

    @TableField("member_name")
    private String memberName;

    @TableField("member_phone")
    private String memberPhone;

    @TableField("enrollment_status")
    private String enrollmentStatus;

    @TableField("enrollment_time")
    private LocalDateTime enrollmentTime;

    @TableField("remark")
    private String remark;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;
    @TableField("enrollment_type")
    private String enrollmentType; // training | activity
    @TableField("course_id")
    private Long courseId; // 关联的课程ID
}