package com.example.mybatis.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("permission")
public class Permission {
    private Long id;
    private String name;
    private String code;
    private String type;
    private String path;
    private String component;   // ✅ 新增
    private Long parentId;      // ✅ 新增
    private Integer orderNum;
    private String createTime;
    private String icon;
    private String extra;
}