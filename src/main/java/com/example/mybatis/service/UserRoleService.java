package com.example.mybatis.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.mybatis.entity.UserRole;
import java.util.List;
/**
 * @author yihui
 */ // ✅ 继承 IService，自动获得 save/remove 等方法
public interface UserRoleService extends IService<UserRole> {
    List<Long> listRoleIdsByUserId(Long userId);
}