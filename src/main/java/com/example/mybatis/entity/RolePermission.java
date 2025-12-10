package com.example.mybatis.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * @author yihui
 */
@Data
@TableName("role_permission")
public class RolePermission {
    @TableField("role_id")
    private Long roleId;
    @TableField("permission_id")
    private Long permissionId;
}
