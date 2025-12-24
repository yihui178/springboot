package com.example.mybatis.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.mybatis.dto.ActivityEnrollmentDTO;
import com.example.mybatis.entity.ActivityEnrollment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 活动报名 Mapper 接口
 * @author yihui
 */
@Mapper
public interface ActivityEnrollmentMapper extends BaseMapper<ActivityEnrollment> {
    /**
     * ✅ 修改返回类型为 ActivityEnrollmentDTO
     * 根据活动ID查询报名列表（包含课程信息）
     */
    List<ActivityEnrollmentDTO> selectByActivityId(@Param("activityId") Long activityId);

    /**
     * ✅ 修改返回类型为 ActivityEnrollmentDTO
     * 根据会员ID查询报名列表（包含课程信息）
     */
    List<ActivityEnrollmentDTO> selectByMemberId(@Param("memberId") Long memberId);
}