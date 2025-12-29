package com.example.mybatis.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.mybatis.common.HttpResult;
import com.example.mybatis.common.SpringException;
import com.example.mybatis.dto.ActivityDTO;
import com.example.mybatis.entity.Activity;
import com.example.mybatis.mapper.ActivityMapper;
import com.example.mybatis.service.ActivityService;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 活动服务实现类
 * @author yihui
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ActivityServiceImpl extends ServiceImpl<ActivityMapper, Activity> implements ActivityService {

    private final ActivityMapper activityMapper;

    // ========== 查询操作 ==========

    @Override
    public PageInfo<ActivityDTO> pageActivitiesWithDTO(int page, int pageSize, String keyword) {
        try {
            PageHelper.startPage(page, pageSize);
            List<Activity> activityList = activityMapper.selectByKeyword(keyword);
            PageInfo<Activity> activityPageInfo = new PageInfo<>(activityList);

            if (activityList.isEmpty()) {
                return createEmptyPageInfo(activityPageInfo);
            }

            return convertToPageInfo(activityPageInfo);
        } catch (Exception e) {
            log.error("查询活动分页失败", e);
            throw new SpringException("查询活动分页失败: " + e.getMessage(), 500, e);
        }
    }

    // ========== CUD 操作 ==========

    @Override
    @Transactional(rollbackFor = Exception.class)
    public HttpResult<String> addActivity(ActivityDTO dto) {
        validateActivityDTO(dto);

        Activity activity = convertToEntity(dto);
        // 初始报名人数为0
        activity.setCurrentParticipants(0);

        boolean success = this.save(activity);
        if (!success) {
            return HttpResult.error(500, "新增活动失败");
        }

        return HttpResult.ok("新增成功");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public HttpResult<String> updateActivity(ActivityDTO dto) {
        if (dto.getId() == null) {
            return HttpResult.error(400, "活动ID不能为空");
        }

        Activity existingActivity = this.getById(dto.getId());
        if (existingActivity == null) {
            return HttpResult.error(404, "活动不存在");
        }

        validateActivityDTO(dto);

        Activity activity = convertToEntity(dto);
        // 保留当前报名人数
        activity.setCurrentParticipants(existingActivity.getCurrentParticipants());

        boolean success = this.updateById(activity);
        if (!success) {
            return HttpResult.error(500, "更新活动失败");
        }

        return HttpResult.ok("更新成功");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public HttpResult<String> deleteActivity(Long activityId) {
        if (activityId == null) {
            return HttpResult.error(400, "活动ID不能为空");
        }

        Activity existingActivity = this.getById(activityId);
        if (existingActivity == null) {
            return HttpResult.error(404, "活动不存在");
        }

        // 检查是否有报名记录
        if (existingActivity.getCurrentParticipants() > 0) {
            return HttpResult.error(400, "该活动已有人报名，无法删除");
        }

        boolean success = this.removeById(activityId);
        if (!success) {
            return HttpResult.error(500, "删除活动失败");
        }

        return HttpResult.ok("删除成功");
    }

    // ========== 私有辅助方法 ==========

    /**
     * 创建空分页结果
     */
    private PageInfo<ActivityDTO> createEmptyPageInfo(PageInfo<Activity> activityPageInfo) {
        PageInfo<ActivityDTO> emptyPage = new PageInfo<>();
        emptyPage.setList(Collections.emptyList());
        emptyPage.setPageNum(activityPageInfo.getPageNum());
        emptyPage.setPageSize(activityPageInfo.getPageSize());
        emptyPage.setTotal(activityPageInfo.getTotal());
        emptyPage.setPages(activityPageInfo.getPages());
        return emptyPage;
    }

    /**
     * 转换分页数据
     */
    private PageInfo<ActivityDTO> convertToPageInfo(PageInfo<Activity> activityPageInfo) {
        List<ActivityDTO> dtoList = activityPageInfo.getList().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        PageInfo<ActivityDTO> dtoPage = new PageInfo<>();
        dtoPage.setList(dtoList);
        dtoPage.setPageNum(activityPageInfo.getPageNum());
        dtoPage.setPageSize(activityPageInfo.getPageSize());
        dtoPage.setTotal(activityPageInfo.getTotal());
        dtoPage.setPages(activityPageInfo.getPages());
        return dtoPage;
    }

    /**
     * 参数校验
     */
    private void validateActivityDTO(ActivityDTO dto) {
        if (dto.getActivityName() == null || dto.getActivityName().trim().isEmpty()) {
            throw new SpringException("活动名称不能为空", 400);
        }
        if (dto.getActivityType() == null || dto.getActivityType().trim().isEmpty()) {
            throw new SpringException("活动类型不能为空", 400);
        }
        if (dto.getDescription() == null || dto.getDescription().trim().isEmpty()) {
            throw new SpringException("活动描述不能为空", 400);
        }
        if (dto.getStartTime() == null) {
            throw new SpringException("开始时间不能为空", 400);
        }
        if (dto.getLocation() == null || dto.getLocation().trim().isEmpty()) {
            throw new SpringException("活动地点不能为空", 400);
        }
    }

    /**
     * Entity 转 DTO
     */
    private ActivityDTO convertToDTO(Activity activity) {
        ActivityDTO dto = new ActivityDTO();
        BeanUtils.copyProperties(activity, dto);
        return dto;
    }

    /**
     * DTO 转 Entity
     */
    private Activity convertToEntity(ActivityDTO dto) {
        Activity activity = new Activity();
        BeanUtils.copyProperties(dto, activity);
        return activity;
    }
}