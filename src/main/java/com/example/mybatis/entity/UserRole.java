package com.example.mybatis.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * @author yihui
 */
@Data
@TableName("user_role")
public class UserRole {
    private Long userId;
    private Long roleId;
}
