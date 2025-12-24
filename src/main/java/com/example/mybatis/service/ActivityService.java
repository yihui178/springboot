package com.example.mybatis.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.mybatis.common.HttpResult;
import com.example.mybatis.dto.ActivityDTO;
import com.example.mybatis.entity.Activity;
import com.github.pagehelper.PageInfo;

/**
 * 活动服务接口
 * @author yihui
 */
public interface ActivityService extends IService<Activity> {
    /**
     * 分页查询活动（返回 DTO）
     */
    PageInfo<ActivityDTO> pageActivitiesWithDTO(int page, int pageSize, String keyword);

    /**
     * 新增活动
     */
    HttpResult<String> addActivity(ActivityDTO dto);

    /**
     * 更新活动
     */
    HttpResult<String> updateActivity(ActivityDTO dto);

    /**
     * 删除活动
     */
    HttpResult<String> deleteActivity(Long activityId);
}