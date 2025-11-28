package com.example.mybatis.controller;

import com.example.mybatis.common.HttpResult;
import com.example.mybatis.dto.NewsDTO;
import com.example.mybatis.service.NewsService;
import com.github.pagehelper.PageInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * @author yihui
 */
@RestController
@RequestMapping("/news")
@RequiredArgsConstructor
@Tag(name = "新闻管理", description = "新闻的增删改查与分页接口")
public class NewsController {

    private final NewsService newsService;

    @GetMapping("/page")
    @Operation(summary = "分页查询新闻")
    public HttpResult<PageInfo<NewsDTO>> page(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "6") Integer pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category) {
        return HttpResult.ok(newsService.pageNewsWithDTO(page, pageSize, keyword, category));
    }

    @PostMapping("/add")
    @Operation(summary = "新增新闻")
    public HttpResult<String> add(@Valid @RequestBody NewsDTO dto) {
        return newsService.addNews(dto);
    }

    @PutMapping("/update")
    @Operation(summary = "更新新闻")
    public HttpResult<String> update(@Valid @RequestBody NewsDTO dto) {
        return newsService.updateNews(dto);
    }

    @PostMapping("/delete")
    @Operation(summary = "删除新闻")
    public HttpResult<String> delete(@RequestBody NewsDTO dto) {
        return newsService.deleteNews(dto.getId());
    }
}