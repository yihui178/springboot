package com.example.mybatis.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.mybatis.entity.Highlight;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface HighlightMapper extends BaseMapper<Highlight> {
    /**
     * 🔥 查询有多少课程引用了该亮点
     */
    @Select("SELECT COUNT(*) FROM course_highlight WHERE highlight_id = #{highlightId}")
    int countCoursesByHighlightId(@Param("highlightId") Long highlightId);
}