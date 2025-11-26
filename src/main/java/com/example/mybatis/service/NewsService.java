package com.example.mybatis.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.mybatis.common.HttpResult;
import com.example.mybatis.dto.NewsDTO;
import com.example.mybatis.entity.News;
import com.github.pagehelper.PageInfo;

/**
 * 新闻服务接口
 * @author yihui
 */
public interface NewsService extends IService<News> {
    /**
     * 分页查询新闻（返回 DTO）
     */
    PageInfo<NewsDTO> pageNewsWithDTO(int page, int pageSize, String keyword, String category);

    /**
     * 新增新闻
     */
    HttpResult<String> addNews(NewsDTO dto);

    /**
     * 更新新闻
     */
    HttpResult<String> updateNews(NewsDTO dto);

    /**
     * 删除新闻（逻辑删除）
     */
    HttpResult<String> deleteNews(Long newsId);
}