package com.example.mybatis.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import javax.swing.text.Highlighter;
import java.util.ArrayList;
import java.util.List;

@Data
@TableName("course")
public class Course {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;
    private String courseName;
    private String category;
    private String description;
    private Boolean online;
//    private String highlightStr;


    @TableField(exist = false) // 这个注解告诉 MyBatis-Plus 这个字段不在数据库表中
    private List<Highlight> highlights; // 新增多对多关联字段

}


