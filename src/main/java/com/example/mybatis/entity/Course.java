package com.example.mybatis.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.List;

/**
 * 课程实体类
 * @author yihui
 */
@Data
@TableName("course")
public class Course {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    private String courseName;

    private String category;

    private String description;

    private Boolean online;
    // 上课时间
    @TableField("schedule_time")
    private String scheduleTime;
    // 课程时长
    @TableField("duration")
    private String duration;
    // 讲师
    @TableField("instructor")
    private String instructor;
    // 上课地点
    @TableField("location")
    private String location;
    //最大报名人数
    @TableField("max_students")
    private Integer maxStudents;
    // 联系电话
    @TableField("contact_phone")
    private String contactPhone;

    // 多对多关联字段（不在数据库表中）
    @TableField(exist = false)
    private List<Highlight> highlights;
}