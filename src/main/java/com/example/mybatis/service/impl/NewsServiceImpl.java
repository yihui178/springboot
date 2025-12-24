package com.example.mybatis.service.impl;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.mybatis.common.HttpResult;
import com.example.mybatis.common.SpringException;
import com.example.mybatis.dto.NewsDTO;
import com.example.mybatis.entity.News;
import com.example.mybatis.entity.User;
import com.example.mybatis.mapper.NewsMapper;
import com.example.mybatis.service.NewsService;
import com.example.mybatis.service.UserService;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
@Service
@RequiredArgsConstructor
public class NewsServiceImpl extends ServiceImpl<NewsMapper, News> implements NewsService {
    private final NewsMapper newsMapper;
    private final UserService userService; // ✅ 添加
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
            throw new SpringException("查询新闻分页失败: " + e.getMessage(), 500, e);
        }
    }
    // ✅ 新增：管理员分页查询
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
            throw new SpringException("查询新闻分页失败: " + e.getMessage(), 500, e);
        }
    }
    @Override
    @Transactional(rollbackFor = Exception.class)
    public HttpResult<String> addNews(NewsDTO dto, Long userId, boolean isAdmin) {
        validateNewsDTO(dto); // ✅ 添加校验

        News news = convertToEntity(dto);
        User creator = userService.getById(userId);
        news.setCreatorId(userId);
        news.setCreatorName(creator.getName());
        news.setStatus(isAdmin ? "approved" : "pending");
        news.setDeleted(false);
        this.save(news);
        return HttpResult.ok(isAdmin ? "发布成功" : "提交成功，请等待审核");
    }
    @Override
    @Transactional(rollbackFor = Exception.class) // ✅ 添加事务
    public HttpResult<String> reviewNews(Long newsId, String action) {
        News news = this.getById(newsId);

        // ✅ 添加空值检查
        if (news == null || news.getDeleted()) {
            throw new SpringException("动态不存在", 404);
        }

        if (!"pending".equals(news.getStatus())) {
            return HttpResult.error(400, "该动态已审核");
        }

        news.setStatus("approve".equals(action) ? "approved" : "rejected");
        this.updateById(news);
        return HttpResult.ok("审核成功");
    }
    @Override
    @Transactional(rollbackFor = Exception.class)
    public HttpResult<String> updateNews(NewsDTO dto) {
        if (dto.getId() == null) {
            throw new SpringException("新闻ID不能为空", 400);
        }
        News existingNews = this.getById(dto.getId());
        if (existingNews == null || existingNews.getDeleted()) {
            throw new SpringException("新闻不存在", 404);
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
            throw new SpringException("新闻ID不能为空", 400);
        }
        News existingNews = this.getById(newsId);
        if (existingNews == null) {
            throw new SpringException("新闻不存在", 404);
        }
//        逻辑删除
//        existingNews.setDeleted(true);
//        this.updateById(existingNews);
        //改为物理删除（直接从数据库删除记录）
        this.removeById(newsId);
        return HttpResult.ok("删除成功");
    }
    // ========== 私有辅助方法 ==========
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
            throw new SpringException("新闻名称不能为空", 400);
        }
        if (dto.getNewsName().length() > 20) {
            throw new SpringException("新闻名称不能超过20个字", 400);
        }
        if (dto.getNewsContent() == null || dto.getNewsContent().trim().isEmpty()) {
            throw new SpringException("新闻内容不能为空", 400);
        }
        if (dto.getNewsContent().length() > 200) {
            throw new SpringException("新闻内容不能超过200个字", 400);
        }
        if (dto.getNewsCategory() == null || dto.getNewsCategory().isEmpty()) {
            throw new SpringException("新闻分类不能为空", 400);
        }
        if (dto.getNewsDescription() == null || dto.getNewsDescription().trim().isEmpty()) {
            throw new SpringException("新闻简介不能为空", 400);
        }
        if (dto.getHasImage() == null) {
            throw new SpringException("是否有配图不能为空", 400);
        }
        if (dto.getNewsTags() == null || dto.getNewsTags().isEmpty()) {
            throw new SpringException("新闻标签不能为空", 400);
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