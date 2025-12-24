package com.example.mybatis.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.mybatis.entity.Member;
import com.example.mybatis.entity.User;
import com.example.mybatis.mapper.UserMapper;
import com.example.mybatis.service.MemberService;
import com.example.mybatis.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author yh
 * @since 2025-09-29
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    /**
     * 使用 @Lazy 避免循环依赖
     * UserService → MemberService → UserRoleService → RoleService
     */
    @Lazy
    private final MemberService memberService;

    /**
     * 检查用户是否为会员（带缓存）
     * 缓存 key: user:member:status::123
     * 缓存时间: 30分钟（在 CacheConfig 中配置）
     */
    @Cacheable(value = "user:member:status", key = "#userId")
    @Override
    public Boolean isMember(Long userId) {
        // 查询 member 表中是否存在该用户
        return memberService.lambdaQuery()
                .eq(Member::getUserId, userId)
                .exists();
    }
}