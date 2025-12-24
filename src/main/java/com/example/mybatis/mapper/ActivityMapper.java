package com.example.mybatis.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.mybatis.entity.Activity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 活动 Mapper 接口
 * @author yihui
 */
@Mapper
public interface ActivityMapper extends BaseMapper<Activity> {
    /**
     * 根据关键词查询活动（用于分页）
     */
    List<Activity> selectByKeyword(@Param("keyword") String keyword);
}