package com.example.mybatis.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.mybatis.common.HttpResult;
import com.example.mybatis.dto.MemberDTO;
import com.example.mybatis.entity.Member;
import com.example.mybatis.mapper.MemberMapper;
import com.example.mybatis.service.MemberService;
import com.example.mybatis.service.NotificationService;
import com.example.mybatis.utils.AdminNotificationUtil;  // ✅ 导入工具类
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @author yihui
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MemberServiceImpl extends ServiceImpl<MemberMapper, Member>
        implements MemberService {

    private final MemberMapper memberMapper;
    private final CacheManager cacheManager;
    private final NotificationService notificationService;
    private final AdminNotificationUtil adminNotificationUtil;

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

    @Override
    @CacheEvict(value = "user:member:status", key = "#dto.userId")
    @Transactional(rollbackFor = Exception.class)
    public HttpResult<String> addMember(MemberDTO dto) {
        if (this.lambdaQuery().eq(Member::getUserId, dto.getUserId()).exists()) {
            return HttpResult.error(400, "该用户已经是会员");
        }
        if (!this.save(toEntity(dto))) {
            return HttpResult.error(500, "新增会员失败");
        }

        // 发送欢迎通知给新会员
        try {
            notificationService.sendNotification(
                    dto.getUserId(),
                    "欢迎加入俱乐部！",
                    String.format(
                            "恭喜 %s 成为俱乐部正式会员！您现在可以参加各类活动、报名课程，享受会员专属权益。",
                            dto.getMemberName()
                    ),
                    "SYSTEM"
            );
            log.info("已发送欢迎通知给新会员: userId={}", dto.getUserId());
        } catch (Exception e) {
            log.error("发送欢迎通知失败", e);
        }

        // 使用工具类通知管理员
        adminNotificationUtil.notifyAllAdmins(
                "新会员加入",
                String.format("新会员【%s】已加入俱乐部", dto.getMemberName()),
                "SYSTEM"
        );

        return HttpResult.ok("新增成功");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public HttpResult<String> updateMember(MemberDTO dto) {
        if (dto.getId() == null) {
            return HttpResult.error(400, "会员ID不能为空");
        }
        Member member = this.getById(dto.getId());
        if (member == null) {
            return HttpResult.error(404, "会员不存在");
        }
        if (this.lambdaQuery()
                .eq(Member::getPhone, dto.getPhone())
                .ne(Member::getId, dto.getId())
                .exists()) {
            return HttpResult.error(400, "该手机号已被其他会员使用");
        }
        if (!this.updateById(toEntity(dto))) {
            return HttpResult.error(500, "更新会员失败");
        }
        return HttpResult.ok("更新成功");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public HttpResult<String> deleteMember(Long memberId) {
        if (memberId == null) {
            return HttpResult.error(400, "会员ID不能为空");
        }
        Member member = this.getById(memberId);
        if (member == null) {
            return HttpResult.error(404, "会员不存在");
        }
        if (!this.removeById(memberId)) {
            return HttpResult.error(500, "删除会员失败");
        }

        clearMemberCache(member.getUserId());

        // 发送会员资格取消通知
        try {
            notificationService.sendNotification(
                    member.getUserId(),
                    "会员资格已取消",
                    "您的俱乐部会员资格已被取消，如有疑问请联系管理员。",
                    "SYSTEM"
            );
            log.info("已发送会员资格取消通知: userId={}", member.getUserId());
        } catch (Exception e) {
            log.error("发送取消通知失败", e);
        }

        return HttpResult.ok("删除成功");
    }
    private void clearMemberCache(Long userId) {
        if (userId == null) {return;}
        cacheManager.getCache("user:member:status").evict(userId);
        log.info("用户 {} 的会员缓存已清除", userId);
    }

    private MemberDTO toDTO(Member member) {
        MemberDTO dto = new MemberDTO();
        BeanUtils.copyProperties(member, dto);
        return dto;
    }

    private Member toEntity(MemberDTO dto) {
        Member member = new Member();
        BeanUtils.copyProperties(dto, member);
        return member;
    }

    @Override
    @Cacheable(value = "user:member:status", key = "#userId")
    public Boolean isUserMember(Long userId) {
        if (userId == null) {
            return false;
        }
        return this.lambdaQuery()
                .eq(Member::getUserId, userId)
                .exists();
    }
}