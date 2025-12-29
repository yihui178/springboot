package com.example.mybatis.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.mybatis.entity.Notification;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * @author yihui
 */
@Mapper
public interface NotificationMapper extends BaseMapper<Notification> {
    /**
     * 统计用户未读通知数量
     */
    int countUnreadByUserId(@Param("userId") Long userId);
}