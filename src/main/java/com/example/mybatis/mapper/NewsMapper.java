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
     * 根据关键词和分类查询新闻（用于分页）
     */
    List<News> selectByKeywordAndCategory(
            @Param("keyword") String keyword,
            @Param("category") String category
    );
}