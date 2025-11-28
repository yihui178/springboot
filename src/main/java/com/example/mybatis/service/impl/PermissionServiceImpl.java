package com.example.mybatis.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.mybatis.entity.Permission;
import com.example.mybatis.mapper.PermissionMapper;
import com.example.mybatis.service.PermissionService;
import org.springframework.stereotype.Service;


/**
 * @author yihui
 */
@Service
// 继承 ServiceImpl，自动实现 IService 的所有方法
public class PermissionServiceImpl
        extends ServiceImpl<PermissionMapper, Permission>
        implements PermissionService {
}
