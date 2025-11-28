package com.example.mybatis.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.mybatis.common.HttpResult;
import com.example.mybatis.common.SpringException;
import com.example.mybatis.dto.NewsDTO;
import com.example.mybatis.entity.News;
import com.example.mybatis.mapper.NewsMapper;
import com.example.mybatis.service.NewsService;
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

/**
 * @author yihui
 */
@Service
@RequiredArgsConstructor
public class NewsServiceImpl extends ServiceImpl<NewsMapper, News> implements NewsService {

    private final NewsMapper newsMapper;

    /**
     * 分页查询新闻（返回 DTO）
     */
    @Override
    public PageInfo<NewsDTO> pageNewsWithDTO(int page, int pageSize, String keyword, String category) {
        try {
            // 使用 try-with-resources 管理 Page 资源
            try (Page<Object> ignored = PageHelper.startPage(page, pageSize)) {
                List<News> newsList = newsMapper.selectByKeywordAndCategory(keyword, category);
                PageInfo<News> newsPageInfo = new PageInfo<>(newsList);

                // 边界处理：如果新闻列表为空
                if (newsList.isEmpty()) {
                    return createEmptyPageInfo(newsPageInfo);
                }

                // 转换为 DTO 并封装分页结果
                return convertToPageInfo(newsPageInfo);
            }
        } catch (Exception e) {
            throw new SpringException("查询新闻分页失败: " + e.getMessage(), 500, e);
        }
    }

    /**
     * 新增新闻
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public HttpResult<String> addNews(NewsDTO dto) {
        validateNewsDTO(dto);
        News news = convertToEntity(dto);
        news.setDeleted(false);
        this.save(news);
        return HttpResult.ok("新增成功");
    }

    /**
     * 更新新闻
     */
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

    /**
     * 删除新闻（逻辑删除）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public HttpResult<String> deleteNews(Long newsId) {
        if (newsId == null) {
            throw new SpringException("新闻ID不能为空", 400);
        }
        News existingNews = this.getById(newsId);
        if (existingNews == null || existingNews.getDeleted()) {
            throw new SpringException("新闻不存在", 404);
        }
        existingNews.setDeleted(true);
        this.updateById(existingNews);
        return HttpResult.ok("删除成功");
    }

    // ========== 私有辅助方法 ==========

    /**
     * 创建空分页结果
     */
    private PageInfo<NewsDTO> createEmptyPageInfo(PageInfo<News> newsPageInfo) {
        PageInfo<NewsDTO> emptyPage = new PageInfo<>();
        emptyPage.setList(Collections.emptyList());
        emptyPage.setPageNum(newsPageInfo.getPageNum());
        emptyPage.setPageSize(newsPageInfo.getPageSize());
        emptyPage.setTotal(newsPageInfo.getTotal());
        emptyPage.setPages(newsPageInfo.getPages());
        return emptyPage;
    }

    /**
     * 转换分页数据
     */
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

    /**
     * 参数校验
     */
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

    /**
     * Entity 转 DTO
     */
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

    /**
     * DTO 转 Entity
     */
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