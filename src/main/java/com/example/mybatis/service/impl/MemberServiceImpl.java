package com.example.mybatis.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.mybatis.common.HttpResult;
import com.example.mybatis.common.SpringException;
import com.example.mybatis.dto.MemberDTO;
import com.example.mybatis.entity.Member;
import com.example.mybatis.mapper.MemberMapper;
import com.example.mybatis.service.MemberService;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 会员服务实现类
 * @author yihui
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MemberServiceImpl extends ServiceImpl<MemberMapper, Member>
        implements MemberService {

    private final MemberMapper memberMapper;
    private final CacheManager cacheManager;

    // ========== 查询 ==========

    @Override
    public PageInfo<MemberDTO> pageMembersWithDTO(int page, int pageSize, String keyword) {
        PageHelper.startPage(page, pageSize);
        List<Member> list = memberMapper.selectByKeyword(keyword);
        PageInfo<Member> pageInfo = new PageInfo<>(list);

        return new PageInfo<>(
                pageInfo.getList().stream().map(this::toDTO).toList()
        ){{
            setPageNum(pageInfo.getPageNum());
            setPageSize(pageInfo.getPageSize());
            setTotal(pageInfo.getTotal());
            setPages(pageInfo.getPages());
        }};
    }

    // ========== 新增 ==========

    @Override
    @CacheEvict(value = "user:member:status", key = "#dto.userId")
    @Transactional(rollbackFor = Exception.class)
    public HttpResult<String> addMember(MemberDTO dto) {
        // 检查是否已是会员
        if (this.lambdaQuery().eq(Member::getUserId, dto.getUserId()).exists()) {
            return HttpResult.error(400, "该用户已经是会员");
        }

        // 保存会员
        if (!this.save(toEntity(dto))) {
            return HttpResult.error(500, "新增会员失败");
        }

        log.info("用户 {} 已成为会员", dto.getUserId());
        return HttpResult.ok("新增成功");
    }

    // ========== 更新 ==========

    @Override
    @Transactional(rollbackFor = Exception.class)
    public HttpResult<String> updateMember(MemberDTO dto) {
        if (dto.getId() == null) {
            return HttpResult.error(400, "会员ID不能为空");
        }

        // 检查会员是否存在
        Member member = this.getById(dto.getId());
        if (member == null) {
            return HttpResult.error(404, "会员不存在");
        }

        // 检查手机号是否重复
        if (this.lambdaQuery()
                .eq(Member::getPhone, dto.getPhone())
                .ne(Member::getId, dto.getId())
                .exists()) {
            return HttpResult.error(400, "该手机号已被其他会员使用");
        }

        // 更新会员
        if (!this.updateById(toEntity(dto))) {
            return HttpResult.error(500, "更新会员失败");
        }

        return HttpResult.ok("更新成功");
    }

    // ========== 删除 ==========

    @Override
    @Transactional(rollbackFor = Exception.class)
    public HttpResult<String> deleteMember(Long memberId) {
        if (memberId == null) {
            return HttpResult.error(400, "会员ID不能为空");
        }

        // 获取会员信息
        Member member = this.getById(memberId);
        if (member == null) {
            return HttpResult.error(404, "会员不存在");
        }

        // 删除会员
        if (!this.removeById(memberId)) {
            return HttpResult.error(500, "删除会员失败");
        }

        // 清除缓存
        clearMemberCache(member.getUserId());

        return HttpResult.ok("删除成功");
    }

    // ========== 私有方法 ==========

    /**
     * 清除会员状态缓存
     */
    private void clearMemberCache(Long userId) {
        if (userId == null) return;

        var cache = cacheManager.getCache("user:member:status");
        if (cache != null) {
            cache.evict(userId);
            log.info("用户 {} 的会员缓存已清除", userId);
        }
    }

    /**
     * Entity 转 DTO
     */
    private MemberDTO toDTO(Member member) {
        MemberDTO dto = new MemberDTO();
        BeanUtils.copyProperties(member, dto);
        return dto;
    }

    /**
     * DTO 转 Entity
     */
    private Member toEntity(MemberDTO dto) {
        Member member = new Member();
        BeanUtils.copyProperties(dto, member);
        return member;
    }
}