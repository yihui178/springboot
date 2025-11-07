package com.example.mybatis.service;

import java.util.List;

public interface RolePermissionService {
    List<Long> listPermissionIdsByRoleIds(List<Long> roleIds);
}
