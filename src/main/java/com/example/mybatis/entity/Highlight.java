package com.example.mybatis.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * @author yihui
 */
@Data
@TableName("highlight")
public class Highlight {
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;
    private String name;

}
