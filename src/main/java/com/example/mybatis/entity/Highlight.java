package com.example.mybatis.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("highlight")
public class Highlight {
    @TableId(value ="id",type = IdType.AUTO)
    private Long id;
    private String name;

}
