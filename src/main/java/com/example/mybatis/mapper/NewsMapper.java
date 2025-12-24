package com.example.mybatis.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.mybatis.entity.News;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * @author yihui
 */
@Mapper
public interface NewsMapper extends BaseMapper<News> {
    /**
     * 根据关键词和分类查询新闻（普通用户/会员）
     */
    List<News> selectByKeywordAndCategory(
            @Param("keyword") String keyword,
            @Param("category") String category
    );

    /**
     * 管理员查询所有状态的动态
     */
    List<News> selectAllForAdmin(
            @Param("keyword") String keyword,
            @Param("category") String category,
            @Param("status") String status
    );
}