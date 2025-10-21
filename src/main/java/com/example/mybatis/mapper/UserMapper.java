package com.example.mybatis.mapper;

import com.example.mybatis.entity.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author yh
 * @since 2025-09-29
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

}
