package com.example.mybatis.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.mybatis.entity.RolePermission;
import com.example.mybatis.mapper.RolePermissionMapper;
import com.example.mybatis.service.RolePermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RolePermissionServiceImpl implements RolePermissionService {

    @Autowired
    private RolePermissionMapper rolePermissionMapper;

    @Override
    public List<Long> listPermissionIdsByRoleIds(List<Long> roleIds) {
        return rolePermissionMapper.selectList(new QueryWrapper<RolePermission>().in("role_id", roleIds))
                .stream()
                .map(RolePermission::getPermissionId)
                .collect(Collectors.toList());
    }
}
