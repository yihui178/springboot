package com.example.mybatis.service;

import java.util.List;

public interface UserRoleService {
    List<Long> listRoleIdsByUserId(Long userId);
}
