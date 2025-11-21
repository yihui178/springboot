package com.example.mybatis.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.mybatis.entity.UserRole;
import com.example.mybatis.mapper.UserRoleMapper;
import com.example.mybatis.service.UserRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
// ✅ 继承 ServiceImpl，自动实现 save/remove 等方法
public class UserRoleServiceImpl extends ServiceImpl<UserRoleMapper, UserRole>
        implements UserRoleService {
    @Autowired
    private UserRoleMapper userRoleMapper;
    @Override
    public List<Long> listRoleIdsByUserId(Long userId) {
        return userRoleMapper.selectList(
                        new QueryWrapper<UserRole>().eq("user_id", userId)
                ).stream()
                .map(UserRole::getRoleId)
                .collect(Collectors.toList());
    }
}
