package com.example.mybatis.service;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.mybatis.common.HttpResult;
import com.example.mybatis.dto.MemberDTO;
import com.example.mybatis.entity.Member;
import com.github.pagehelper.PageInfo;
/**
 * 会员服务接口
 * @author yihui
 */
public interface MemberService extends IService<Member> {
    /**
     * 分页查询会员（返回 DTO）
     */
    PageInfo<MemberDTO> pageMembersWithDTO(int page, int pageSize, String keyword);

    /**
     * 新增会员
     */
    HttpResult<String> addMember(MemberDTO dto);

    /**
     * 更新会员
     */
    HttpResult<String> updateMember(MemberDTO dto);

    /**
     * 删除会员
     */
    HttpResult<String> deleteMember(Long memberId);
}