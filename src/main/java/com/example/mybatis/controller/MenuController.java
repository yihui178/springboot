package com.example.mybatis.controller;
import com.example.mybatis.entity.Permission;
import com.example.mybatis.common.HttpResult;
import com.example.mybatis.service.PermissionService;
import com.example.mybatis.service.RolePermissionService;
import com.example.mybatis.service.UserRoleService;
import com.example.mybatis.utils.RequestUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import java.util.*;
/**
 * Vben Admin 菜单接口控制器
 * @author yihui
 */
@Slf4j
@RestController
@RequestMapping("/menu")
@RequiredArgsConstructor
public class MenuController {
    private static final ObjectMapper JSON = new ObjectMapper();

    private final UserRoleService userRoleService;
    private final RolePermissionService rolePermissionService;
    private final PermissionService permissionService;
    /**
     * 获取菜单树（Vben 兼容格式）
     */
    @GetMapping("/all")
    public HttpResult<List<Map<String, Object>>> getAllMenus(HttpServletRequest request) {
        try {
            // 使用工具类获取 userId
            Long userId = RequestUtils.getCurrentUserId(request);
            // 查询用户角色
            List<Long> roleIds = userRoleService.listRoleIdsByUserId(userId);
            if (roleIds.isEmpty()) {
                return HttpResult.ok(Collections.emptyList());
            }
            // 查询角色权限
            List<Long> permIds = rolePermissionService.listPermissionIdsByRoleIds(roleIds);
            if (permIds.isEmpty()) {
                return HttpResult.ok(Collections.emptyList());
            }
            // 过滤菜单权限
            List<Permission> perms = permissionService.listByIds(permIds).stream()
                    .filter(p -> "menu".equalsIgnoreCase(p.getType()))
                    .sorted(Comparator.comparingInt(Permission::getOrderNum))
                    .toList();
            // 构建节点映射
            Map<Long, Map<String, Object>> nodeMap = new LinkedHashMap<>();
            for (Permission p : perms) {
                nodeMap.put(p.getId(), buildNode(p));
            }
            // 构建树结构
            List<Map<String, Object>> roots = new ArrayList<>();
            for (Permission p : perms) {
                Map<String, Object> node = nodeMap.get(p.getId());
                if (p.getParentId() == null) {
                    roots.add(node);
                } else {
                    Map<String, Object> parent = nodeMap.get(p.getParentId());
                    if (parent != null) {
                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> children =
                                (List<Map<String, Object>>) parent.get("children");
                        children.add(node);
                    } else {
                        roots.add(node);
                    }
                }
            }
            return HttpResult.ok(roots);
        } catch (Exception e) {
            log.error("加载菜单失败", e);
            return HttpResult.error(500, "加载菜单失败：" + e.getMessage());
        }
    }
    /**
     * 构建单个菜单节点
     */
    private Map<String, Object> buildNode(Permission p) {
        Map<String, Object> node = new LinkedHashMap<>();
        node.put("id", p.getId());
        node.put("name", p.getName());
        node.put("path", p.getPath().startsWith("/") ? p.getPath() : "/" + p.getPath());
        // 解析 extra
        Map<String, Object> extra = parseExtra(p.getExtra());
        // 计算 component
        String component = resolveComponent(p, extra);
        node.put("component", component);
        // meta 信息
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("title", p.getName());
        meta.put("icon", Optional.ofNullable(p.getIcon()).orElse("lucide:menu"));
        meta.put("code", p.getCode());
        if (extra.containsKey("link")) {
            meta.put("link", extra.get("link"));
        }
        node.put("meta", meta);
        node.put("extra", extra);
        node.put("children", new ArrayList<Map<String, Object>>());
        return node;
    }
    /**
     * 根据类型和外链自动选择组件
     */
    private String resolveComponent(Permission p, Map<String, Object> extra) {
        String c = p.getComponent();
        if (p.getParentId() == null) {
            // 顶级菜单
            return (c != null && !c.isBlank() && !"LAYOUT".equalsIgnoreCase(c))
                    ? trimLeadingSlash(c)
                    : "BasicLayout";
        }
        // 外链 / iframe
        if ("IFrameView".equalsIgnoreCase(c) || extra.containsKey("link")) {
            return "IFrameView";
        }
        return trimLeadingSlash(Optional.ofNullable(c).orElse(""));
    }
    /**
     * 解析 extra JSON 或 redirect 语法
     */
    private Map<String, Object> parseExtra(String extraStr) {
        Map<String, Object> extra = new LinkedHashMap<>();
        if (extraStr == null || extraStr.isBlank()) {
            return extra;
        }
        try {
            if (extraStr.startsWith("{")) {
                extra = JSON.readValue(extraStr, new TypeReference<>() {});
            } else if (extraStr.startsWith("redirect:")) {
                extra.put("redirect", extraStr.substring("redirect:".length()));
            }
        } catch (Exception e) {
            log.warn("解析 extra 失败: {}", extraStr, e);
        }
        return extra;
    }
    /**
     * 去除字符串开头的斜杠
     */
    private String trimLeadingSlash(String s) {
        return s != null && s.startsWith("/") ? s.substring(1) : s;
    }
}