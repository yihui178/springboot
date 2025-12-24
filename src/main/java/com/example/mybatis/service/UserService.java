package com.example.mybatis.service;

import com.example.mybatis.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author yh
 * @since 2025-09-29
 */
public interface UserService extends IService<User> {

    /**
     * 检查用户是否为会员（带缓存）
     * @param userId 用户ID
     * @return true=是会员，false=不是会员
     */
    Boolean isMember(Long userId);
}