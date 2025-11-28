package com.example.mybatis.service.impl;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.mybatis.common.HttpResult;
import com.example.mybatis.common.SpringException;
import com.example.mybatis.dto.HighlightDTO;
import com.example.mybatis.entity.Highlight;
import com.example.mybatis.mapper.HighlightMapper;
import com.example.mybatis.service.HighlightService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;
/**
 * 亮点服务实现类
 * @author yihui
 */
@Service
@RequiredArgsConstructor
public class HighlightServiceImpl extends ServiceImpl<HighlightMapper, Highlight>
        implements HighlightService {

    private final HighlightMapper highlightMapper;

    @Override
    public List<HighlightDTO> listAllHighlights() {
        try {
            List<Highlight> highlights = this.list();
            return highlights.stream().map(highlight -> {
                HighlightDTO dto = new HighlightDTO();
                BeanUtils.copyProperties(highlight, dto);
                return dto;
            }).collect(Collectors.toList());
        } catch (Exception e) {
            throw new SpringException("查询亮点列表失败: " + e.getMessage(), 500, e);
        }
    }
    @Override
    @Transactional(rollbackFor = Exception.class)
    public HttpResult<String> addHighlight(HighlightDTO dto) {
        Highlight highlight = convertToEntity(dto);
        boolean success = this.save(highlight);

        if (!success) {
            return HttpResult.error(500, "新增亮点失败");
        }

        return HttpResult.ok("新增成功");
    }
    @Override
    @Transactional(rollbackFor = Exception.class)
    public HttpResult<String> updateHighlight(HighlightDTO dto) {
        if (dto.getId() == null) {
            return HttpResult.error(400, "亮点ID不能为空");
        }

        Highlight existingHighlight = this.getById(dto.getId());
        if (existingHighlight == null) {
            return HttpResult.error(404, "亮点不存在");
        }

        Highlight highlight = convertToEntity(dto);
        boolean success = this.updateById(highlight);

        if (!success) {
            return HttpResult.error(500, "更新亮点失败");
        }

        return HttpResult.ok("更新成功");
    }
    @Override
    @Transactional(rollbackFor = Exception.class)
    public HttpResult<String> deleteHighlight(Long highlightId) {
        if (highlightId == null) {
            return HttpResult.error(400, "亮点ID不能为空");
        }

        Highlight existingHighlight = this.getById(highlightId);
        if (existingHighlight == null) {
            return HttpResult.error(404, "亮点不存在");
        }

        int referenceCount = highlightMapper.countCoursesByHighlightId(highlightId);
        if (referenceCount > 0) {
            return HttpResult.error(400, "该亮点已被 " + referenceCount + " 个课程引用，无法删除");
        }

        boolean success = this.removeById(highlightId);
        if (!success) {
            return HttpResult.error(500, "删除亮点失败");
        }

        return HttpResult.ok("删除成功");
    }
    private Highlight convertToEntity(HighlightDTO dto) {
        Highlight highlight = new Highlight();
        BeanUtils.copyProperties(dto, highlight);
        return highlight;
    }
}