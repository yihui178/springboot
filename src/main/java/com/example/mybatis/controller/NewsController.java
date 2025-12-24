package com.example.mybatis.controller;

import com.example.mybatis.common.HttpResult;
import com.example.mybatis.dto.NewsDTO;
import com.example.mybatis.entity.Member;
import com.example.mybatis.service.MemberService;
import com.example.mybatis.service.NewsService;
import com.example.mybatis.service.UserRoleService;
import com.example.mybatis.service.RoleService;
import com.example.mybatis.utils.RequestUtils;
import com.github.pagehelper.PageInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

/**
 * 新闻管理控制器
 * @author yihui
 */
@RestController
@RequestMapping("/news")
@RequiredArgsConstructor
@Tag(name = "新闻管理", description = "新闻的增删改查与分页接口")
public class NewsController {
    private final NewsService newsService;
    private final UserRoleService userRoleService;
    private final RoleService roleService;
    private final MemberService memberService;

    /**
     * 普通用户/会员分页查询（只看已通过的）
     */
    @GetMapping("/page")
    @Operation(summary = "分页查询新闻")
    public HttpResult<PageInfo<NewsDTO>> page(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "6") Integer pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category) {
        return HttpResult.ok(newsService.pageNewsWithDTO(page, pageSize, keyword, category));
    }

    /**
     * 管理员分页查询（可查看所有状态）
     */
    @GetMapping("/page-admin")
    @Operation(summary = "管理员分页查询")
    public HttpResult<PageInfo<NewsDTO>> pageAdmin(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "6") Integer pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status,
            HttpServletRequest request) {
        Long userId = RequestUtils.getCurrentUserId(request);

        // 只检查管理员权限
        if (!isAdmin(userId)) {
            return HttpResult.error(403, "无权访问");
        }

        return HttpResult.ok(newsService.pageNewsForAdmin(page, pageSize, keyword, category, status));
    }

    /**
     * 新增新闻（会员和管理员）
     */
    @PostMapping("/add")
    @Operation(summary = "新增新闻")
    public HttpResult<String> add(@Valid @RequestBody NewsDTO dto, HttpServletRequest request) {
        Long userId = RequestUtils.getCurrentUserId(request);

        // 分别检查管理员和会员身份
        boolean isAdminUser = isAdmin(userId);
        boolean isMemberUser = isMember(userId);

        // 管理员或会员都可以发布
        if (!isAdminUser && !isMemberUser) {
            return HttpResult.error(403, "只有会员才能发布动态");
        }

        // 管理员发布直接通过，会员发布需要审核
        return newsService.addNews(dto, userId, isAdminUser);
    }

    /**
     * 更新新闻（只有管理员）
     */
    @PutMapping("/update")
    @Operation(summary = "更新新闻")
    public HttpResult<String> update(@Valid @RequestBody NewsDTO dto, HttpServletRequest request) {
        Long userId = RequestUtils.getCurrentUserId(request);

        // 只检查管理员权限
        if (!isAdmin(userId)) {
            return HttpResult.error(403, "只有管理员可以编辑");
        }

        return newsService.updateNews(dto);
    }

    /**
     * 删除新闻（只有管理员）
     */
    @PostMapping("/delete")
    @Operation(summary = "删除新闻")
    public HttpResult<String> delete(@RequestBody NewsDTO dto, HttpServletRequest request) {
        Long userId = RequestUtils.getCurrentUserId(request);

        // 只检查管理员权限
        if (!isAdmin(userId)) {
            return HttpResult.error(403, "只有管理员可以删除");
        }

        return newsService.deleteNews(dto.getId());
    }

    /**
     * 审核动态
     */
    @PostMapping("/review")
    @Operation(summary = "审核动态")
    public HttpResult<String> review(@RequestBody Map<String, Object> data, HttpServletRequest request) {
        Long userId = RequestUtils.getCurrentUserId(request);

        // 只检查管理员权限
        if (!isAdmin(userId)) {
            return HttpResult.error(403, "只有管理员可以审核");
        }

        Long newsId = Long.valueOf(data.get("newsId").toString());
        String action = data.get("action").toString();
        return newsService.reviewNews(newsId, action);
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 判断是否为管理员（查询 role 表）
     * 用于检查用户是否有管理员权限（admin 或 super）
     */
    private boolean isAdmin(Long userId) {
        try {
            // 1. 查询用户的角色ID列表
            List<Long> roleIds = userRoleService.listRoleIdsByUserId(userId);
            if (roleIds.isEmpty()) {
                return false;
            }

            // 2. 查询角色代码
            List<String> roleCodes = roleService.listByIds(roleIds).stream()
                    .map(role -> role.getCode())
                    .toList();

            // 3. 判断是否包含管理员角色
            return roleCodes.contains("admin") || roleCodes.contains("super");
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 判断是否为会员（查询 member 表）
     * 用于检查用户是否是俱乐部会员
     */
    private boolean isMember(Long userId) {
        try {
            // 查询 member 表
            Member member = memberService.lambdaQuery()
                    .eq(Member::getUserId, userId)
                    .one();
            return member != null;
        } catch (Exception e) {
            return false;
        }
    }
}