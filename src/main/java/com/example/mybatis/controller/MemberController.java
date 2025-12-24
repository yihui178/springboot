package com.example.mybatis.controller;
import com.example.mybatis.common.HttpResult;
import com.example.mybatis.dto.MemberDTO;
import com.example.mybatis.service.MemberService;
import com.github.pagehelper.PageInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
/**
 * 会员管理控制器
 * @author yihui
 */
@RestController
@RequestMapping("/member")
@RequiredArgsConstructor
@Tag(name = "会员管理", description = "会员的增删改查与分页接口")
public class MemberController {

    private final MemberService memberService;

    /**
     * 分页查询会员
     */
    @GetMapping("/page")
    @Operation(summary = "分页查询会员")
    public HttpResult<PageInfo<MemberDTO>> page(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword) {
        return HttpResult.ok(memberService.pageMembersWithDTO(page, pageSize, keyword));
    }

    /**
     * 新增会员
     */
    @PostMapping("/add")
    @Operation(summary = "新增会员")
    public HttpResult<String> add(@Valid @RequestBody MemberDTO dto) {
        return memberService.addMember(dto);
    }

    /**
     * 更新会员
     */
    @PutMapping("/update")
    @Operation(summary = "更新会员")
    public HttpResult<String> update(@Valid @RequestBody MemberDTO dto) {
        return memberService.updateMember(dto);
    }

    /**
     * 删除会员
     */
    @PostMapping("/delete")
    @Operation(summary = "删除会员")
    public HttpResult<String> delete(@RequestBody MemberDTO dto) {
        return memberService.deleteMember(dto.getId());
    }
}