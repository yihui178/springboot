package com.example.mybatis.service;

import java.util.List;

/**
 * @author yihui
 */
public interface RolePermissionService {
    List<Long> listPermissionIdsByRoleIds(List<Long> roleIds);
}
