package com.example.mybatis.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.mybatis.entity.Permission;

import java.util.List;

public interface PermissionService extends IService<Permission> {
    List<String> getPermissionsByRole(String role);
}
