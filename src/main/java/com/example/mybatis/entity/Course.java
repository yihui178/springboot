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

    @TableField(exist = false)
    private List<String> highlights = new ArrayList<>();

    private String highlightStr;

    public void setHighlightStr(String highlightStr) {
        this.highlightStr = highlightStr;
        if (highlightStr != null && !highlightStr.isEmpty()) {
            this.highlights = List.of(highlightStr.split(","));
        } else {
            this.highlights = new ArrayList<>();
        }
    }

    public void setHighlights(List<String> highlights) {
        this.highlights = highlights;
        if (highlights != null && !highlights.isEmpty()) {
            this.highlightStr = String.join(",", highlights);
        } else {
            this.highlightStr = null;
        }
    }
}

