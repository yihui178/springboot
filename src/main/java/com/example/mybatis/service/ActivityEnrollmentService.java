package com.example.mybatis.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.mybatis.common.HttpResult;
import com.example.mybatis.dto.ActivityEnrollmentDTO;
import com.example.mybatis.entity.ActivityEnrollment;

import java.util.List;

/**
 * 活动报名服务接口
 * @author yihui
 */
public interface ActivityEnrollmentService extends IService<ActivityEnrollment> {
    /**
     * 报名活动
     */
    HttpResult<String> enrollActivity(ActivityEnrollmentDTO dto);

    /**
     * 审核报名（通过/拒绝）
     */
    HttpResult<String> reviewEnrollment(Long enrollmentId, String status);

    /**
     * 取消报名
     */
    HttpResult<String> cancelEnrollment(Long enrollmentId);

    /**
     * 获取活动的报名列表
     */
    List<ActivityEnrollmentDTO> getEnrollmentsByActivityId(Long activityId);

    /**
     * 获取会员的报名记录
     */
    List<ActivityEnrollmentDTO> getEnrollmentsByMemberId(Long memberId);
}