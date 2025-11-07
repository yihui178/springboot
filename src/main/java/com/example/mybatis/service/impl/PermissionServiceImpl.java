package com.example.mybatis.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.mybatis.entity.Permission;
import com.example.mybatis.mapper.PermissionMapper;
import com.example.mybatis.service.PermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
// 继承 ServiceImpl，自动实现 IService 的所有方法
public class PermissionServiceImpl extends ServiceImpl<PermissionMapper, Permission>
        implements PermissionService {

    @Autowired
    private PermissionMapper permissionMapper;

    @Override
    public List<String> getPermissionsByRole(String role) {
        List<Permission> permissions = permissionMapper.findPermissionsByRole(role);
        return permissions.stream()
                .map(Permission::getCode) // 确保 Permission 类有 code 字段和 getCode() 方法
                .collect(Collectors.toList());
    }
}
