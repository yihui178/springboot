package com.example.mybatis.service.impl;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.mybatis.common.SpringException;
import com.example.mybatis.dto.HighlightDTO;
import com.example.mybatis.entity.Highlight;
import com.example.mybatis.mapper.HighlightMapper;
import com.example.mybatis.service.HighlightService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;
/**
 * 亮点服务实现类
 */
@Service
public class HighlightServiceImpl extends ServiceImpl<HighlightMapper, Highlight>
        implements HighlightService {
    @Autowired
    private HighlightMapper highlightMapper;
    // ========== 公共接口实现 ==========
    /**
     * 查询所有亮点（返回 DTO）
     */
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
    /**
     * 新增亮点
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addHighlight(HighlightDTO dto) {
        // 1. DTO 转 Entity
        Highlight highlight = convertToEntity(dto);
        // 2. 保存亮点
        if (!this.save(highlight)) {
            throw new SpringException("新增亮点失败", 500);
        }
    }
    /**
     * 更新亮点
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateHighlight(HighlightDTO dto) {
        // 1. 校验亮点 ID
        if (dto.getId() == null) {
            throw new SpringException("亮点ID不能为空", 400);
        }
        // 2. 检查亮点是否存在
        Highlight existingHighlight = this.getById(dto.getId());
        if (existingHighlight == null) {
            throw new SpringException("亮点不存在", 404);
        }
        // 3. DTO 转 Entity
        Highlight highlight = convertToEntity(dto);
        // 4. 更新亮点
        if (!this.updateById(highlight)) {
            throw new SpringException("更新亮点失败", 500);
        }
    }
    /**
     * 删除亮点
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteHighlight(Long highlightId) {
        // 1. 校验亮点 ID
        if (highlightId == null) {
            throw new SpringException("亮点ID不能为空", 400);
        }
        // 2. 检查亮点是否存在
        Highlight existingHighlight = this.getById(highlightId);
        if (existingHighlight == null) {
            throw new SpringException("亮点不存在", 404);
        }
        // 3. 检查是否有课程引用（关键业务逻辑）
        int referenceCount = highlightMapper.countCoursesByHighlightId(highlightId);
        if (referenceCount > 0) {
            throw new SpringException(
                    "该亮点已被 " + referenceCount + " 个课程引用，无法删除",
                    400
            );
        }
        // 4. 删除亮点
        if (!this.removeById(highlightId)) {
            throw new SpringException("删除亮点失败", 500);
        }
    }
    // ========== 私有方法：业务逻辑封装 ==========
    /**
     * DTO 转 Entity
     */
    private Highlight convertToEntity(HighlightDTO dto) {
        Highlight highlight = new Highlight();
        BeanUtils.copyProperties(dto, highlight);
        return highlight;
    }
}