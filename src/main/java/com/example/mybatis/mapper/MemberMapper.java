package com.example.mybatis.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.mybatis.entity.Member;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;
/**
 * 会员 Mapper 接口
 * @author yihui
 */
@Mapper
public interface MemberMapper extends BaseMapper<Member> {
    /**
     * 根据关键词查询会员（用于分页）
     */
    List<Member> selectByKeyword(@Param("keyword") String keyword);
}