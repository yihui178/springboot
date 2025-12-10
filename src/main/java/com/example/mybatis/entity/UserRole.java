package com.example.mybatis.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * @author yihui
 */
@Data
@TableName("user_role")
public class UserRole {
    @TableField("user_id")
    private Long userId;
    @TableField("role_id")
    private Long roleId;
}
