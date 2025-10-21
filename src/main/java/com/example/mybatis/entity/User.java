package com.example.mybatis.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;
import java.io.Serializable;

import jdk.jfr.Experimental;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>
 * 
 * </p>
 *
 * @author yh
 * @since 2025-09-29
 */

@Data
@TableName("user")
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @ExcelProperty("id")
    @TableId("id")
    private Long id;

    /**
     * 姓名
     */
    @ExcelProperty("姓名")
    @TableField("name")
    private String name;

    /**
     * 年龄
     */
    @ExcelProperty("年龄")
    @TableField("age")
    private Integer age;

    /**
     * 邮箱
     */
    @ExcelProperty("邮箱")
    @TableField("email")
    private String email;
}
