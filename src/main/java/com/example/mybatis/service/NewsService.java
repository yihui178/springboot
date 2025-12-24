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
     * 分页查询新闻（普通用户/会员）
     */
    PageInfo<NewsDTO> pageNewsWithDTO(int page, int pageSize, String keyword, String category);

    /**
     * ✅ 管理员分页查询
     */
    PageInfo<NewsDTO> pageNewsForAdmin(int page, int pageSize, String keyword, String category, String status);

    /**
     * ✅ 新增新闻（会员和管理员）
     */
    HttpResult<String> addNews(NewsDTO dto, Long userId, boolean isAdmin);

    /**
     * 更新新闻
     */
    HttpResult<String> updateNews(NewsDTO dto);

    /**
     * 删除新闻
     */
    HttpResult<String> deleteNews(Long newsId);

    /**
     * ✅ 审核动态
     */
    HttpResult<String> reviewNews(Long newsId, String action);
}