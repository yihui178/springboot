package com.example.mybatis.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@TableName("course")
public class Course {
    private Long id;
    private String courseName;
    private String category;
    private String description;
    private Boolean online;
    private String highlightStr;
}

