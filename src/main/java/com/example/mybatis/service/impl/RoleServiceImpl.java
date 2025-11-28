package com.example.mybatis.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.mybatis.entity.Role;
import com.example.mybatis.mapper.RoleMapper;
import com.example.mybatis.service.RoleService;
import org.springframework.stereotype.Service;

/**
 * @author yihui
 */
@Service  // 这里使用 @Service 注解让 Spring 管理此类
public class RoleServiceImpl extends ServiceImpl<RoleMapper, Role> implements RoleService {
}
