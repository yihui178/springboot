package com.example.mybatis.service.impl;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.mybatis.common.HttpResult;
import com.example.mybatis.dto.NewsDTO;
import com.example.mybatis.entity.News;
import com.example.mybatis.entity.Role;
import com.example.mybatis.entity.User;
import com.example.mybatis.entity.UserRole;
import com.example.mybatis.mapper.NewsMapper;
import com.example.mybatis.service.*;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
@Slf4j
@Service
@RequiredArgsConstructor
public class NewsServiceImpl extends ServiceImpl<NewsMapper, News> implements NewsService {
    private final NewsMapper newsMapper;
    private final UserService userService;
    private final NotificationService notificationService;
    private final RoleService roleService;
    private final UserRoleService userRoleService;
    // 向所有管理员发送通知
    private void notifyAllAdmins(String title, String content, String type) {
        try {
            // 1. 查询管理员角色（admin 和 super）
            List<Role> adminRoles = roleService.lambdaQuery()
                    .in(Role::getCode, "admin", "super")
                    .list();
            if (adminRoles.isEmpty()) {
                log.warn("⚠️ 未找到管理员角色");
                return;
            }
            // 2. 获取管理员角色的 ID 列表
            List<Long> adminRoleIds = adminRoles.stream()
                    .map(Role::getId)
                    .collect(Collectors.toList());
            // 3. 查询所有管理员的 userId
            List<Long> adminUserIds = userRoleService.lambdaQuery()
                    .in(UserRole::getRoleId, adminRoleIds)
                    .list()
                    .stream()
                    .map(UserRole::getUserId)
                    .distinct()
                    .collect(Collectors.toList());
            if (adminUserIds.isEmpty()) {
                log.warn("⚠️ 未找到管理员用户");
                return;
            }
            // 4. 批量发送通知
            int successCount = 0;
            for (Long adminUserId : adminUserIds) {
                try {
                    notificationService.sendNotification(
                            adminUserId,
                            title,
                            content,
                            type
                    );
                    successCount++;
                } catch (Exception e) {
                    log.error("❌ 向管理员 {} 发送通知失败", adminUserId, e);
                }
            }

        } catch (Exception e) {
            log.error("❌ 批量发送通知给管理员失败", e);
        }
    }
    @Override
    public PageInfo<NewsDTO> pageNewsWithDTO(int page, int pageSize, String keyword, String category) {
        try {
            try (Page<Object> ignored = PageHelper.startPage(page, pageSize)) {
                List<News> newsList = newsMapper.selectByKeywordAndCategory(keyword, category);
                PageInfo<News> newsPageInfo = new PageInfo<>(newsList);
                if (newsList.isEmpty()) {
                    return createEmptyPageInfo(newsPageInfo);
                }
                return convertToPageInfo(newsPageInfo);
            }
        } catch (Exception e) {
            throw new RuntimeException("查询新闻分页失败: " + e.getMessage(), e);
        }
    }
    @Override
    public PageInfo<NewsDTO> pageNewsForAdmin(int page, int pageSize, String keyword, String category, String status) {
        try {
            try (Page<Object> ignored = PageHelper.startPage(page, pageSize)) {
                List<News> newsList = newsMapper.selectAllForAdmin(keyword, category, status);
                PageInfo<News> newsPageInfo = new PageInfo<>(newsList);
                if (newsList.isEmpty()) {
                    return createEmptyPageInfo(newsPageInfo);
                }
                return convertToPageInfo(newsPageInfo);
            }
        } catch (Exception e) {
            throw new RuntimeException("查询新闻分页失败: " + e.getMessage(), e);
        }
    }
    @Override
    @Transactional(rollbackFor = Exception.class)
    public HttpResult<String> addNews(NewsDTO dto, Long userId, boolean isAdmin) {
        validateNewsDTO(dto);
        News news = convertToEntity(dto);
        User creator = userService.getById(userId);
        news.setCreatorId(userId);
        news.setCreatorName(creator.getName());
        news.setStatus(isAdmin ? "approved" : "pending");
        news.setDeleted(false);
        this.save(news);
        // 如果是会员发布，通知所有管理员审核
        if (!isAdmin) {
            notifyAllAdmins(
                    "新动态待审核",
                    String.format("%s 发布了新动态【%s】，请及时审核",
                            creator.getName(),
                            news.getNewsName()),
                    "NEWS"
            );
        }
        return HttpResult.ok(isAdmin ? "发布成功" : "提交成功，请等待审核");
    }
    @Override
    @Transactional(rollbackFor = Exception.class)
    public HttpResult<String> reviewNews(Long newsId, String action) {
        News news = this.getById(newsId);
        if (news == null || news.getDeleted()) {
            return HttpResult.error(404, "动态不存在");
        }
        if (!"pending".equals(news.getStatus())) {
            return HttpResult.error(400, "该动态已审核");
        }
        news.setStatus("approve".equals(action) ? "approved" : "rejected");
        this.updateById(news);
        // 发送审核结果通知给创建者
        if (news.getCreatorId() != null) {
            try {
                String title = "approve".equals(action) ? "动态审核通过" : "动态审核未通过";
                String content = String.format(
                        "您发布的动态【%s】%s",
                        news.getNewsName(),
                        "approve".equals(action) ? "已通过审核并发布" : "未通过审核，请修改后重新提交"
                );
                notificationService.sendNotification(
                        news.getCreatorId(),
                        title,
                        content,
                        "NEWS"
                );

            } catch (Exception e) {
                log.error("❌ 发送审核通知失败", e);
            }
        }
        return HttpResult.ok("审核成功");
    }
    @Override
    @Transactional(rollbackFor = Exception.class)
    public HttpResult<String> updateNews(NewsDTO dto) {
        if (dto.getId() == null) {
            throw new RuntimeException("新闻ID不能为空");
        }
        News existingNews = this.getById(dto.getId());
        if (existingNews == null || existingNews.getDeleted()) {
            throw new RuntimeException("新闻不存在");
        }
        validateNewsDTO(dto);
        News news = convertToEntity(dto);
        news.setDeleted(existingNews.getDeleted());
        this.updateById(news);
        return HttpResult.ok("更新成功");
    }
    @Override
    @Transactional(rollbackFor = Exception.class)
    public HttpResult<String> deleteNews(Long newsId) {
        if (newsId == null) {
            throw new RuntimeException("新闻ID不能为空");
        }
        News existingNews = this.getById(newsId);
        if (existingNews == null || existingNews.getDeleted()) {
            throw new RuntimeException("新闻不存在");
        }
        existingNews.setDeleted(true);
        this.updateById(existingNews);
        return HttpResult.ok("删除成功");
    }
    // ========== 私有方法保持不变 ==========
    private PageInfo<NewsDTO> createEmptyPageInfo(PageInfo<News> newsPageInfo) {
        PageInfo<NewsDTO> emptyPage = new PageInfo<>();
        emptyPage.setList(Collections.emptyList());
        emptyPage.setPageNum(newsPageInfo.getPageNum());
        emptyPage.setPageSize(newsPageInfo.getPageSize());
        emptyPage.setTotal(newsPageInfo.getTotal());
        emptyPage.setPages(newsPageInfo.getPages());
        return emptyPage;
    }
    private PageInfo<NewsDTO> convertToPageInfo(PageInfo<News> newsPageInfo) {
        List<NewsDTO> dtoList = newsPageInfo.getList().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        PageInfo<NewsDTO> dtoPage = new PageInfo<>();
        dtoPage.setList(dtoList);
        dtoPage.setPageNum(newsPageInfo.getPageNum());
        dtoPage.setPageSize(newsPageInfo.getPageSize());
        dtoPage.setTotal(newsPageInfo.getTotal());
        dtoPage.setPages(newsPageInfo.getPages());
        return dtoPage;
    }
    private void validateNewsDTO(NewsDTO dto) {
        if (dto.getNewsName() == null || dto.getNewsName().trim().isEmpty()) {
            throw new RuntimeException("新闻名称不能为空");
        }
        if (dto.getNewsName().length() > 20) {
            throw new RuntimeException("新闻名称不能超过20个字");
        }
        if (dto.getNewsContent() == null || dto.getNewsContent().trim().isEmpty()) {
            throw new RuntimeException("新闻内容不能为空");
        }
        if (dto.getNewsContent().length() > 200) {
            throw new RuntimeException("新闻内容不能超过200个字");
        }
        if (dto.getNewsCategory() == null || dto.getNewsCategory().isEmpty()) {
            throw new RuntimeException("新闻分类不能为空");
        }
        if (dto.getNewsDescription() == null || dto.getNewsDescription().trim().isEmpty()) {
            throw new RuntimeException("新闻简介不能为空");
        }
        if (dto.getHasImage() == null) {
            throw new RuntimeException("是否有配图不能为空");
        }
        if (dto.getNewsTags() == null || dto.getNewsTags().isEmpty()) {
            throw new RuntimeException("新闻标签不能为空");
        }
    }
    private NewsDTO convertToDTO(News news) {
        NewsDTO dto = new NewsDTO();
        BeanUtils.copyProperties(news, dto);
        if (news.getNewsCategory() != null && !news.getNewsCategory().isEmpty()) {
            dto.setNewsCategory(Arrays.asList(news.getNewsCategory().split(",")));
        } else {
            dto.setNewsCategory(Collections.emptyList());
        }
        if (news.getNewsTags() != null && !news.getNewsTags().isEmpty()) {
            dto.setNewsTags(Arrays.asList(news.getNewsTags().split(",")));
        } else {
            dto.setNewsTags(Collections.emptyList());
        }
        return dto;
    }
    private News convertToEntity(NewsDTO dto) {
        News news = new News();
        BeanUtils.copyProperties(dto, news);
        if (dto.getNewsCategory() != null && !dto.getNewsCategory().isEmpty()) {
            news.setNewsCategory(String.join(",", dto.getNewsCategory()));
        }
        if (dto.getNewsTags() != null && !dto.getNewsTags().isEmpty()) {
            news.setNewsTags(String.join(",", dto.getNewsTags()));
        }
        if (Boolean.FALSE.equals(dto.getHasImage())) {
            news.setImageUrl("");
        }
        return news;
    }
}